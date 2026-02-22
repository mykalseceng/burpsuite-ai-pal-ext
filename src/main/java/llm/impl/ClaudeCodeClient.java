package llm.impl;

import static base.Api.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import llm.LLMClient;
import llm.LLMResponse;
import ui.chat.ChatMessage;
import util.CliEnvironmentUtil;
import util.Utf16Sanitizer;

public class ClaudeCodeClient implements LLMClient {
    private static final long DEFAULT_TIMEOUT_SECONDS = 120;

    private final String claudePath;
    private final String model;

    public ClaudeCodeClient(String claudePath, String model) {
        this.claudePath = claudePath;
        this.model = model;
    }

    @Override
    public LLMResponse complete(String prompt, String systemPrompt) {
        try {
            String fullPrompt = buildSinglePrompt(prompt, systemPrompt);
            return runClaude(fullPrompt);
        } catch (Exception e) {
            api.logging().logToError("Claude Code error: " + e.getMessage());
            return LLMResponse.error("Claude Code error: " + e.getMessage());
        }
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, String newMessage) {
        try {
            String prompt = buildConversationPrompt(messages, newMessage);
            return runClaude(prompt);
        } catch (Exception e) {
            api.logging().logToError("Claude Code chat error: " + e.getMessage());
            return LLMResponse.error("Claude Code chat error: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public void chatStreaming(List<ChatMessage> messages, String newMessage, StreamCallback callback) {
        Process process = null;
        try {
            String prompt = buildConversationPrompt(messages, newMessage);

            List<String> command = buildCommand("stream-json");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            pb.directory(new java.io.File(System.getProperty("user.home")));
            CliEnvironmentUtil.ensureNodePath(pb);

            process = pb.start();
            final Process proc = process;

            // Write prompt to stdin
            try (OutputStream os = proc.getOutputStream()) {
                os.write(Utf16Sanitizer.sanitize(prompt).getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // Read streaming JSON from stdout
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (callback.isCancelled()) {
                        proc.destroyForcibly();
                        return;
                    }

                    if (line.trim().isEmpty()) continue;

                    try {
                        JsonObject event = JsonParser.parseString(line).getAsJsonObject();
                        String type = event.has("type") ? event.get("type").getAsString() : "";

                        switch (type) {
                            case "assistant" -> {
                                // Content block with text
                                if (event.has("message")) {
                                    JsonObject message = event.getAsJsonObject("message");
                                    if (message.has("content")) {
                                        var contentArray = message.getAsJsonArray("content");
                                        for (var element : contentArray) {
                                            JsonObject contentBlock = element.getAsJsonObject();
                                            if ("text".equals(contentBlock.has("type")
                                                    ? contentBlock.get("type").getAsString() : "")) {
                                                String text = contentBlock.get("text").getAsString();
                                                if (!text.isEmpty()) {
                                                    callback.onChunk(text);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            case "content_block_delta" -> {
                                if (event.has("delta")) {
                                    JsonObject delta = event.getAsJsonObject("delta");
                                    if ("text_delta".equals(delta.has("type")
                                            ? delta.get("type").getAsString() : "")) {
                                        String text = delta.has("text") ? delta.get("text").getAsString() : "";
                                        if (!text.isEmpty()) {
                                            callback.onChunk(text);
                                        }
                                    }
                                }
                            }
                            case "message_delta" -> {
                                // End of message - may contain usage info
                                if (event.has("usage")) {
                                    JsonObject usage = event.getAsJsonObject("usage");
                                    int tokens = 0;
                                    if (usage.has("input_tokens")) tokens += usage.get("input_tokens").getAsInt();
                                    if (usage.has("output_tokens")) tokens += usage.get("output_tokens").getAsInt();
                                }
                            }
                            case "result" -> {
                                // Final result event
                                int tokens = 0;
                                if (event.has("usage")) {
                                    JsonObject usage = event.getAsJsonObject("usage");
                                    if (usage.has("input_tokens")) tokens += usage.get("input_tokens").getAsInt();
                                    if (usage.has("output_tokens")) tokens += usage.get("output_tokens").getAsInt();
                                }
                                if (!callback.isCancelled()) {
                                    callback.onComplete(tokens);
                                }
                                return;
                            }
                        }
                    } catch (Exception e) {
                        api.logging().logToError("Failed to parse Claude Code streaming chunk: " + line);
                    }
                }
            }

            // Read stderr for errors
            String stderr = readStream(proc.getErrorStream());
            boolean exited = proc.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!exited) {
                proc.destroyForcibly();
                if (!callback.isCancelled()) {
                    callback.onError("Claude Code timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
                }
                return;
            }

            int exitCode = proc.exitValue();
            if (exitCode != 0 && !callback.isCancelled()) {
                callback.onError("Claude Code exited with code " + exitCode
                        + (stderr.isEmpty() ? "" : ": " + stderr));
                return;
            }

            // If we didn't get a result event, complete now
            if (!callback.isCancelled()) {
                callback.onComplete(0);
            }

        } catch (Exception e) {
            if (process != null) {
                process.destroyForcibly();
            }
            if (!callback.isCancelled()) {
                api.logging().logToError("Claude Code streaming error: " + e.getMessage());
                callback.onError("Claude Code streaming error: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean testConnection() {
        try {
            List<String> command = new ArrayList<>();
            command.add(claudePath);
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
            api.logging().logToError("Claude Code connection test failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Claude Code";
    }

    @Override
    public String getModel() {
        return model;
    }

    /**
     * Get the version string from the claude CLI.
     */
    public String getVersion() {
        try {
            List<String> command = new ArrayList<>();
            command.add(claudePath);
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

    private LLMResponse runClaude(String prompt) throws Exception {
        List<String> command = buildCommand("json");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        pb.directory(new java.io.File(System.getProperty("user.home")));
        CliEnvironmentUtil.ensureNodePath(pb);

        Process process = pb.start();

        // Write prompt to stdin (avoids shell injection)
        try (OutputStream os = process.getOutputStream()) {
            os.write(Utf16Sanitizer.sanitize(prompt).getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        boolean exited = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
            return LLMResponse.error("Claude Code timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            return LLMResponse.error("Claude Code exited with code " + exitCode
                    + (stderr.isEmpty() ? "" : ": " + stderr));
        }

        // Parse JSON response
        try {
            JsonObject json = JsonParser.parseString(stdout).getAsJsonObject();
            String result = json.has("result") ? json.get("result").getAsString() : "";
            int tokens = 0;
            if (json.has("usage")) {
                JsonObject usage = json.getAsJsonObject("usage");
                if (usage.has("input_tokens")) tokens += usage.get("input_tokens").getAsInt();
                if (usage.has("output_tokens")) tokens += usage.get("output_tokens").getAsInt();
            }
            return LLMResponse.success(result, tokens);
        } catch (Exception e) {
            // If JSON parsing fails, treat raw stdout as the response
            if (!stdout.trim().isEmpty()) {
                return LLMResponse.success(stdout.trim());
            }
            return LLMResponse.error("Failed to parse Claude Code response: " + e.getMessage());
        }
    }

    private List<String> buildCommand(String outputFormat) {
        List<String> command = new ArrayList<>();
        command.add(claudePath);
        command.add("-p");
        command.add("--output-format");
        command.add(outputFormat);
        if ("stream-json".equals(outputFormat)) {
            command.add("--verbose");
        }
        command.add("--model");
        command.add(model);
        // CRITICAL: Disable all tools to prevent prompt injection attacks.
        // Claude Code is an agent with access to Bash, Read, Write, etc.
        // Since we feed untrusted HTTP traffic into prompts, a malicious payload
        // could trick Claude into executing arbitrary commands without this flag.
        // --tools "" removes all tools from the agent entirely.
        // --allowedTools "" ensures no tools are auto-approved as a second layer.
        command.add("--tools");
        command.add("");
        command.add("--allowedTools");
        command.add("");
        return command;
    }

    private String buildSinglePrompt(String prompt, String systemPrompt) {
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            return systemPrompt + "\n\n" + prompt;
        }
        return prompt;
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
