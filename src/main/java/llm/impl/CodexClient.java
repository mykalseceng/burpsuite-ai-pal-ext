package llm.impl;

import static base.Api.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import llm.LLMClient;
import llm.LLMResponse;
import ui.chat.ChatMessage;
import util.CliEnvironmentUtil;
import util.Utf16Sanitizer;

public class CodexClient implements LLMClient {
    private static final int PROCESS_TIMEOUT_SECONDS = 120;

    private final String codexPath;
    private final String model;

    public CodexClient(String codexPath, String model) {
        this.codexPath = codexPath;
        this.model = model;
    }

    @Override
    public LLMResponse complete(String prompt, String systemPrompt) {
        String fullPrompt = buildSinglePrompt(systemPrompt, prompt);
        return runCodexExec(fullPrompt);
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, String newMessage) {
        String prompt = buildConversationPrompt(messages, newMessage);
        return runCodexExec(prompt);
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public void chatStreaming(List<ChatMessage> messages, String newMessage, StreamCallback callback) {
        String prompt = buildConversationPrompt(messages, newMessage);
        Process process = null;

        try {
            process = startCodexProcess();
            writePromptToStdin(process, prompt);

            int totalTokens = 0;
            Map<String, String> itemTexts = new HashMap<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (callback.isCancelled()) {
                        process.destroyForcibly();
                        return;
                    }
                    if (line.trim().isEmpty()) continue;

                    try {
                        JsonObject event = JsonParser.parseString(line).getAsJsonObject();
                        String type = event.has("type") ? event.get("type").getAsString() : "";

                        if (type.equals("item.started") || type.equals("item.updated") || type.equals("item.completed")) {
                            if (event.has("item")) {
                                JsonObject item = event.getAsJsonObject("item");
                                String itemType = item.has("type") ? item.get("type").getAsString() : "";
                                if (itemType.equals("agent_message") && item.has("text")) {
                                    String itemId = item.has("id") ? item.get("id").getAsString() : "";
                                    String currentText = item.get("text").getAsString();
                                    String previousText = itemTexts.getOrDefault(itemId, "");

                                    if (currentText.length() > previousText.length()) {
                                        String delta = currentText.substring(previousText.length());
                                        callback.onChunk(delta);
                                    }
                                    itemTexts.put(itemId, currentText);
                                }
                            }
                        } else if (type.equals("turn.completed")) {
                            if (event.has("usage")) {
                                JsonObject usage = event.getAsJsonObject("usage");
                                if (usage.has("input_tokens")) {
                                    totalTokens += usage.get("input_tokens").getAsInt();
                                }
                                if (usage.has("output_tokens")) {
                                    totalTokens += usage.get("output_tokens").getAsInt();
                                }
                            }
                        }
                    } catch (Exception e) {
                        api.logging().logToError("Failed to parse Codex JSONL: " + line);
                    }
                }
            }

            boolean exited = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                if (!callback.isCancelled()) {
                    callback.onError("Codex process timed out after " + PROCESS_TIMEOUT_SECONDS + "s");
                }
                return;
            }

            if (!callback.isCancelled()) {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    String stderr = readStream(process.getErrorStream());
                    callback.onError("Codex exited with code " + exitCode + ": " + stderr);
                } else {
                    callback.onComplete(totalTokens);
                }
            }
        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            if (!callback.isCancelled()) {
                api.logging().logToError("Codex streaming error: " + e.getMessage());
                callback.onError("Codex error: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean testConnection() {
        try {
            List<String> command = new ArrayList<>();
            command.add(codexPath);
            command.add("--version");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(new java.io.File(System.getProperty("user.home")));
            CliEnvironmentUtil.ensureNodePath(pb);

            Process process = pb.start();
            boolean exited = process.waitFor(10, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            api.logging().logToError("Codex connection test failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "OpenAI Codex";
    }

    @Override
    public String getModel() {
        return model;
    }

    /**
     * Returns the version string from `codex --version`, or null on failure.
     */
    public String getVersion() {
        try {
            List<String> command = new ArrayList<>();
            command.add(codexPath);
            command.add("--version");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(new java.io.File(System.getProperty("user.home")));
            CliEnvironmentUtil.ensureNodePath(pb);

            Process process = pb.start();
            String output = readStream(process.getInputStream()).trim();
            boolean exited = process.waitFor(10, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                return null;
            }
            return process.exitValue() == 0 ? output : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LLMResponse runCodexExec(String prompt) {
        Process process = null;
        try {
            process = startCodexProcess();
            writePromptToStdin(process, prompt);

            StringBuilder responseText = new StringBuilder();
            int totalTokens = 0;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    try {
                        JsonObject event = JsonParser.parseString(line).getAsJsonObject();
                        String type = event.has("type") ? event.get("type").getAsString() : "";

                        if (type.equals("item.completed") && event.has("item")) {
                            JsonObject item = event.getAsJsonObject("item");
                            String itemType = item.has("type") ? item.get("type").getAsString() : "";
                            if (itemType.equals("agent_message") && item.has("text")) {
                                String text = item.get("text").getAsString();
                                if (!text.isEmpty()) {
                                    if (responseText.length() > 0) responseText.append("\n");
                                    responseText.append(text);
                                }
                            }
                        } else if (type.equals("turn.completed") && event.has("usage")) {
                            JsonObject usage = event.getAsJsonObject("usage");
                            if (usage.has("input_tokens")) {
                                totalTokens += usage.get("input_tokens").getAsInt();
                            }
                            if (usage.has("output_tokens")) {
                                totalTokens += usage.get("output_tokens").getAsInt();
                            }
                        }
                    } catch (Exception e) {
                        api.logging().logToError("Failed to parse Codex JSONL: " + line);
                    }
                }
            }

            boolean exited = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!exited) {
                process.destroyForcibly();
                return LLMResponse.error("Codex process timed out after " + PROCESS_TIMEOUT_SECONDS + "s");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String stderr = readStream(process.getErrorStream());
                return LLMResponse.error("Codex exited with code " + exitCode + ": " + stderr);
            }

            if (responseText.length() == 0) {
                return LLMResponse.error("No response received from Codex CLI");
            }

            return LLMResponse.success(responseText.toString(), totalTokens);
        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            api.logging().logToError("Codex API error: " + e.getMessage());
            return LLMResponse.error("Codex error: " + e.getMessage());
        }
    }

    private Process startCodexProcess() throws Exception {
        List<String> command = new ArrayList<>();
        command.add(codexPath);
        command.add("exec");
        command.add("--json");
        command.add("--ephemeral");
        command.add("--skip-git-repo-check");
        command.add("--sandbox");
        command.add("read-only");
        command.add("-c");
        command.add("features.shell_tool=false");
        command.add("-c");
        command.add("web_search=disabled");
        command.add("-m");
        command.add(model);
        command.add("-"); // read prompt from stdin

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        pb.directory(new java.io.File(System.getProperty("user.home")));
        CliEnvironmentUtil.ensureNodePath(pb);

        return pb.start();
    }

    private void writePromptToStdin(Process process, String prompt) throws Exception {
        String sanitized = Utf16Sanitizer.sanitize(prompt);
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(sanitized.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        }
    }

    private String buildSinglePrompt(String systemPrompt, String userPrompt) {
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            return "System: " + systemPrompt + "\n\nUser: " + userPrompt;
        }
        return userPrompt;
    }

    private String buildConversationPrompt(List<ChatMessage> messages, String newMessage) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            String role = switch (msg.getRole()) {
                case SYSTEM -> "System";
                case USER -> "User";
                case ASSISTANT -> "Assistant";
            };
            sb.append(role).append(": ").append(msg.getFullContent()).append("\n\n");
        }
        if (newMessage != null && !newMessage.isEmpty()) {
            sb.append("User: ").append(newMessage);
        }
        return sb.toString();
    }

    private String readStream(java.io.InputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
