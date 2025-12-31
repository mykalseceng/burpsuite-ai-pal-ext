package llm.impl;

import burp.api.montoya.http.Http;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import llm.LLMClient;
import llm.LLMResponse;
import ui.chat.ChatMessage;
import util.Utf16Sanitizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * LLM client for Ollama - runs models locally.
 * Ollama API: https://github.com/ollama/ollama/blob/main/docs/api.md
 */
public class OllamaClient implements LLMClient {
    private static final String GENERATE_PATH = "/api/generate";
    private static final String CHAT_PATH = "/api/chat";

    private final Http http;
    private final Logging logging;
    private final String baseUrl;
    private final String model;

    private final String host;
    private final int port;
    private final boolean useHttps;

    public OllamaClient(Http http, Logging logging, String baseUrl, String model) {
        this.http = http;
        this.logging = logging;
        this.baseUrl = baseUrl;
        this.model = model;

        // Parse base URL to extract host, port, and protocol
        URI uri = parseBaseUrl(baseUrl);
        this.host = uri.getHost();
        this.port = uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equals("https") ? 443 : 80);
        this.useHttps = uri.getScheme().equals("https");
    }

    private URI parseBaseUrl(String url) {
        try {
            // Ensure URL has a scheme
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            return new URI(url);
        } catch (Exception e) {
            logging.logToError("Invalid Ollama base URL: " + url + ", using default localhost:11434");
            try {
                return new URI("http://localhost:11434");
            } catch (Exception ex) {
                throw new RuntimeException("Failed to parse URL", ex);
            }
        }
    }

    @Override
    public LLMResponse complete(String prompt, String systemPrompt) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("prompt", Utf16Sanitizer.sanitize(prompt));
            requestBody.addProperty("stream", false);

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                requestBody.addProperty("system", Utf16Sanitizer.sanitize(systemPrompt));
            }

            return sendRequest(GENERATE_PATH, requestBody, false);
        } catch (Exception e) {
            logging.logToError("Ollama API error: " + e.getMessage());
            return LLMResponse.error("Ollama API error: " + e.getMessage());
        }
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, String newMessage) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("stream", false);

            JsonArray messagesArray = new JsonArray();

            // Add conversation history
            for (ChatMessage msg : messages) {
                JsonObject msgObj = new JsonObject();
                String role = switch (msg.getRole()) {
                    case SYSTEM -> "system";
                    case USER -> "user";
                    case ASSISTANT -> "assistant";
                };
                msgObj.addProperty("role", role);
                msgObj.addProperty("content", Utf16Sanitizer.sanitize(msg.getFullContent()));
                messagesArray.add(msgObj);
            }

            // Add new user message only if not empty
            if (newMessage != null && !newMessage.isEmpty()) {
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", Utf16Sanitizer.sanitize(newMessage));
                messagesArray.add(userMsg);
            }

            requestBody.add("messages", messagesArray);

            return sendRequest(CHAT_PATH, requestBody, true);
        } catch (Exception e) {
            logging.logToError("Ollama chat error: " + e.getMessage());
            return LLMResponse.error("Ollama chat error: " + e.getMessage());
        }
    }

    private LLMResponse sendRequest(String path, JsonObject requestBody, boolean isChatEndpoint) {
        String body = Utf16Sanitizer.sanitize(requestBody.toString());
        ByteArray bodyBytes = ByteArray.byteArray(body.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.httpRequest()
                .withService(HttpService.httpService(host, port, useHttps))
                .withMethod("POST")
                .withPath(path)
                .withHeader("Host", host + (port != 80 && port != 443 ? ":" + port : ""))
                .withHeader("Content-Type", "application/json")
                .withBody(bodyBytes);

        HttpRequestResponse response = http.sendRequest(request);

        if (response.response() == null) {
            return LLMResponse.error("No response from Ollama. Is Ollama running at " + baseUrl + "?");
        }

        int statusCode = response.response().statusCode();
        // Explicitly decode as UTF-8 to handle special characters correctly
        String responseBody = new String(response.response().body().getBytes(), StandardCharsets.UTF_8);

        if (statusCode != 200) {
            return LLMResponse.error("Ollama API error (HTTP " + statusCode + "): " + responseBody);
        }

        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            String content;
            String thinking = null;

            if (isChatEndpoint) {
                // Chat endpoint returns message.content
                JsonObject message = jsonResponse.getAsJsonObject("message");
                content = message != null && message.has("content")
                        ? message.get("content").getAsString()
                        : "";
                // Check for thinking content (used by reasoning models like DeepSeek-R1)
                if (message != null && message.has("thinking")) {
                    thinking = message.get("thinking").getAsString();
                }
            } else {
                // Generate endpoint returns response directly
                content = jsonResponse.has("response")
                        ? jsonResponse.get("response").getAsString()
                        : "";
                // Check for thinking content at top level
                if (jsonResponse.has("thinking")) {
                    thinking = jsonResponse.get("thinking").getAsString();
                }
            }

            // Prepend thinking content if present
            if (thinking != null && !thinking.isEmpty()) {
                content = "<thinking>\n" + thinking + "\n</thinking>\n\n" + content;
            }

            // Ollama provides token counts in eval_count and prompt_eval_count
            int tokensUsed = 0;
            if (jsonResponse.has("eval_count")) {
                tokensUsed += jsonResponse.get("eval_count").getAsInt();
            }
            if (jsonResponse.has("prompt_eval_count")) {
                tokensUsed += jsonResponse.get("prompt_eval_count").getAsInt();
            }

            return LLMResponse.success(content, tokensUsed);
        } catch (Exception e) {
            logging.logToError("Failed to parse Ollama response: " + e.getMessage());
            return LLMResponse.error("Failed to parse Ollama response: " + e.getMessage());
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public void chatStreaming(List<ChatMessage> messages, String newMessage, StreamCallback callback) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("stream", true);

            JsonArray messagesArray = new JsonArray();

            // Add conversation history
            for (ChatMessage msg : messages) {
                JsonObject msgObj = new JsonObject();
                String role = switch (msg.getRole()) {
                    case SYSTEM -> "system";
                    case USER -> "user";
                    case ASSISTANT -> "assistant";
                };
                msgObj.addProperty("role", role);
                msgObj.addProperty("content", Utf16Sanitizer.sanitize(msg.getFullContent()));
                messagesArray.add(msgObj);
            }

            // Add new user message if present
            if (newMessage != null && !newMessage.isEmpty()) {
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", Utf16Sanitizer.sanitize(newMessage));
                messagesArray.add(userMsg);
            }

            requestBody.add("messages", messagesArray);

            // Use HttpURLConnection for streaming
            String urlStr = (useHttps ? "https://" : "http://") + host + ":" + port + CHAT_PATH;
            URI uri = new URI(urlStr);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(2000); // Short read timeout (2s) for responsive cancel

            String body = Utf16Sanitizer.sanitize(requestBody.toString());
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                callback.onError("Ollama API error (HTTP " + statusCode + ")");
                return;
            }

            StringBuilder thinkingContent = new StringBuilder();
            int totalTokens = 0;
            boolean isFirstChunk = true;
            final HttpURLConnection connection = conn; // final reference for cleanup

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean done = false;
                while (!done) {
                    // Check if cancelled - disconnect to unblock readLine
                    if (callback.isCancelled()) {
                        connection.disconnect();
                        break;
                    }

                    try {
                        line = reader.readLine();
                    } catch (java.net.SocketTimeoutException e) {
                        // Timeout allows us to check cancel flag; continue if not cancelled
                        continue;
                    }

                    if (line == null) break;
                    if (line.trim().isEmpty()) continue;

                    try {
                        JsonObject chunk = JsonParser.parseString(line).getAsJsonObject();

                        // Check if done
                        if (chunk.has("done") && chunk.get("done").getAsBoolean()) {
                            // Get final token counts
                            if (chunk.has("eval_count")) {
                                totalTokens += chunk.get("eval_count").getAsInt();
                            }
                            if (chunk.has("prompt_eval_count")) {
                                totalTokens += chunk.get("prompt_eval_count").getAsInt();
                            }
                            break;
                        }

                        // Extract content from message
                        if (chunk.has("message")) {
                            JsonObject message = chunk.getAsJsonObject("message");

                            // Handle thinking content
                            if (message.has("thinking")) {
                                String thinking = message.get("thinking").getAsString();
                                if (!thinking.isEmpty()) {
                                    if (thinkingContent.length() == 0) {
                                        callback.onChunk("<thinking>\n");
                                    }
                                    thinkingContent.append(thinking);
                                    callback.onChunk(thinking);
                                }
                            }

                            // Handle regular content
                            if (message.has("content")) {
                                String content = message.get("content").getAsString();
                                if (!content.isEmpty()) {
                                    // If we had thinking content, close the tag first
                                    if (thinkingContent.length() > 0 && isFirstChunk) {
                                        callback.onChunk("\n</thinking>\n\n");
                                        isFirstChunk = false;
                                    }
                                    callback.onChunk(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logging.logToError("Failed to parse streaming chunk: " + line);
                    }
                }
            }

            // Only report completion if not cancelled
            if (!callback.isCancelled()) {
                callback.onComplete(totalTokens);
            }

        } catch (Exception e) {
            // Don't report error if cancelled (disconnect causes exception)
            if (!callback.isCancelled()) {
                logging.logToError("Ollama streaming error: " + e.getMessage());
                callback.onError("Ollama streaming error: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean testConnection() {
        LLMResponse response = complete("Say 'OK' if you can read this.", null);
        return response.isSuccess();
    }

    @Override
    public String getProviderName() {
        return "Ollama";
    }

    @Override
    public String getModel() {
        return model;
    }
}
