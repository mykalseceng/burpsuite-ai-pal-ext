package llm.impl;

import static base.Api.api;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import llm.LLMClient;
import llm.LLMResponse;
import ui.chat.ChatMessage;
import util.Utf16Sanitizer;

public class OllamaClient implements LLMClient {
    private static final String GENERATE_PATH = "/api/generate";
    private static final String CHAT_PATH = "/api/chat";

    private final String baseUrl;
    private final String model;
    private final String host;
    private final int port;
    private final boolean useHttps;

    public OllamaClient(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;

        URI uri = parseBaseUrl(baseUrl);
        this.host = uri.getHost();
        this.port = uri.getPort() != -1 ? uri.getPort() : (uri.getScheme().equals("https") ? 443 : 80);
        this.useHttps = uri.getScheme().equals("https");
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
            api.logging().logToError("Ollama API error: " + e.getMessage());
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

            if (newMessage != null && !newMessage.isEmpty()) {
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", Utf16Sanitizer.sanitize(newMessage));
                messagesArray.add(userMsg);
            }

            requestBody.add("messages", messagesArray);

            return sendRequest(CHAT_PATH, requestBody, true);
        } catch (Exception e) {
            api.logging().logToError("Ollama chat error: " + e.getMessage());
            return LLMResponse.error("Ollama chat error: " + e.getMessage());
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

            if (newMessage != null && !newMessage.isEmpty()) {
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", Utf16Sanitizer.sanitize(newMessage));
                messagesArray.add(userMsg);
            }

            requestBody.add("messages", messagesArray);

            String urlStr = (useHttps ? "https://" : "http://") + host + ":" + port + CHAT_PATH;
            URI uri = new URI(urlStr);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);

            String body = Utf16Sanitizer.sanitize(requestBody.toString());
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = conn.getResponseCode();
            if (statusCode != 200) {
                callback.onError("Ollama API error (HTTP " + statusCode + ")");
                return;
            }

            int totalTokens = 0;
            final HttpURLConnection connection = conn;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (true) {
                    if (callback.isCancelled()) {
                        connection.disconnect();
                        break;
                    }

                    try {
                        line = reader.readLine();
                    } catch (java.net.SocketTimeoutException e) {
                        continue;
                    }

                    if (line == null) break;
                    if (line.trim().isEmpty()) continue;

                    try {
                        JsonObject chunk = JsonParser.parseString(line).getAsJsonObject();

                        if (chunk.has("done") && chunk.get("done").getAsBoolean()) {
                            if (chunk.has("eval_count")) {
                                totalTokens += chunk.get("eval_count").getAsInt();
                            }
                            if (chunk.has("prompt_eval_count")) {
                                totalTokens += chunk.get("prompt_eval_count").getAsInt();
                            }
                            break;
                        }

                        if (chunk.has("message")) {
                            JsonObject message = chunk.getAsJsonObject("message");

                            if (message.has("content")) {
                                String content = message.get("content").getAsString();
                                if (!content.isEmpty()) {
                                    callback.onChunk(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        api.logging().logToError("Failed to parse streaming chunk: " + line);
                    }
                }
            }

            if (!callback.isCancelled()) {
                callback.onComplete(totalTokens);
            }

        } catch (Exception e) {
            if (!callback.isCancelled()) {
                api.logging().logToError("Ollama streaming error: " + e.getMessage());
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

    private URI parseBaseUrl(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            return new URI(url);
        } catch (Exception e) {
            api.logging().logToError("Invalid Ollama base URL: " + url + ", using default localhost:11434");
            try {
                return new URI("http://localhost:11434");
            } catch (Exception ex) {
                throw new RuntimeException("Failed to parse URL", ex);
            }
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

        HttpRequestResponse response = api.http().sendRequest(request);

        if (response.response() == null) {
            return LLMResponse.error("No response from Ollama. Is Ollama running at " + baseUrl + "?");
        }

        int statusCode = response.response().statusCode();
        String responseBody = new String(response.response().body().getBytes(), StandardCharsets.UTF_8);

        if (statusCode != 200) {
            return LLMResponse.error("Ollama API error (HTTP " + statusCode + "): " + responseBody);
        }

        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

            String content;

            if (isChatEndpoint) {
                JsonObject message = jsonResponse.getAsJsonObject("message");
                content = message != null && message.has("content")
                        ? message.get("content").getAsString()
                        : "";
            } else {
                content = jsonResponse.has("response")
                        ? jsonResponse.get("response").getAsString()
                        : "";
            }

            int tokensUsed = 0;
            if (jsonResponse.has("eval_count")) {
                tokensUsed += jsonResponse.get("eval_count").getAsInt();
            }
            if (jsonResponse.has("prompt_eval_count")) {
                tokensUsed += jsonResponse.get("prompt_eval_count").getAsInt();
            }

            return LLMResponse.success(content, tokensUsed);
        } catch (Exception e) {
            api.logging().logToError("Failed to parse Ollama response: " + e.getMessage());
            return LLMResponse.error("Failed to parse Ollama response: " + e.getMessage());
        }
    }
}
