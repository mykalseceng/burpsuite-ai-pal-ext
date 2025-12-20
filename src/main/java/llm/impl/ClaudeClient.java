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

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClaudeClient implements LLMClient {
    private static final String API_HOST = "api.anthropic.com";
    private static final String API_PATH = "/v1/messages";
    private static final String API_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 4096;

    private final Http http;
    private final Logging logging;
    private final String apiKey;
    private final String model;

    public ClaudeClient(Http http, Logging logging, String apiKey, String model) {
        this.http = http;
        this.logging = logging;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public LLMResponse complete(String prompt, String systemPrompt) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("max_tokens", MAX_TOKENS);

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                requestBody.addProperty("system", Utf16Sanitizer.sanitize(systemPrompt));
            }

            JsonArray messages = new JsonArray();
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", Utf16Sanitizer.sanitize(prompt));
            messages.add(userMsg);

            requestBody.add("messages", messages);

            return sendRequest(requestBody);
        } catch (Exception e) {
            logging.logToError("Claude API error: " + e.getMessage());
            return LLMResponse.error("Claude API error: " + e.getMessage());
        }
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, String newMessage) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("max_tokens", MAX_TOKENS);

            // Extract system message if present
            for (ChatMessage msg : messages) {
                if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                    requestBody.addProperty("system", Utf16Sanitizer.sanitize(msg.getFullContent()));
                    break;
                }
            }

            JsonArray messagesArray = new JsonArray();

            // Add conversation history (skip system messages)
            for (ChatMessage msg : messages) {
                if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                    continue;
                }

                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", msg.getRole().getApiValue());
                msgObj.addProperty("content", Utf16Sanitizer.sanitize(msg.getFullContent()));
                messagesArray.add(msgObj);
            }

            // Add new user message
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", Utf16Sanitizer.sanitize(newMessage));
            messagesArray.add(userMsg);

            requestBody.add("messages", messagesArray);

            return sendRequest(requestBody);
        } catch (Exception e) {
            logging.logToError("Claude chat error: " + e.getMessage());
            return LLMResponse.error("Claude chat error: " + e.getMessage());
        }
    }

    private LLMResponse sendRequest(JsonObject requestBody) {
        String body = Utf16Sanitizer.sanitize(requestBody.toString());
        ByteArray bodyBytes = ByteArray.byteArray(body.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.httpRequest()
                .withService(HttpService.httpService(API_HOST, 443, true))
                .withMethod("POST")
                .withPath(API_PATH)
                .withHeader("Host", API_HOST)
                .withHeader("x-api-key", apiKey)
                .withHeader("anthropic-version", API_VERSION)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(bodyBytes);

        HttpRequestResponse response = http.sendRequest(request);

        if (response.response() == null) {
            return LLMResponse.error("No response from Claude API");
        }

        int statusCode = response.response().statusCode();
        String responseBody = response.response().bodyToString();

        if (statusCode != 200) {
            return LLMResponse.error("Claude API error (HTTP " + statusCode + "): " + responseBody);
        }

        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray content = jsonResponse.getAsJsonArray("content");
            if (content != null && !content.isEmpty()) {
                StringBuilder textContent = new StringBuilder();
                for (int i = 0; i < content.size(); i++) {
                    JsonObject block = content.get(i).getAsJsonObject();
                    if ("text".equals(block.get("type").getAsString())) {
                        textContent.append(block.get("text").getAsString());
                    }
                }

                int tokensUsed = 0;
                if (jsonResponse.has("usage")) {
                    JsonObject usage = jsonResponse.getAsJsonObject("usage");
                    int inputTokens = usage.has("input_tokens") ? usage.get("input_tokens").getAsInt() : 0;
                    int outputTokens = usage.has("output_tokens") ? usage.get("output_tokens").getAsInt() : 0;
                    tokensUsed = inputTokens + outputTokens;
                }

                return LLMResponse.success(textContent.toString(), tokensUsed);
            }
            return LLMResponse.error("No response content from Claude");
        } catch (Exception e) {
            logging.logToError("Failed to parse Claude response: " + e.getMessage());
            return LLMResponse.error("Failed to parse Claude response: " + e.getMessage());
        }
    }

    @Override
    public boolean testConnection() {
        LLMResponse response = complete("Say 'OK' if you can read this.", null);
        return response.isSuccess();
    }

    @Override
    public String getProviderName() {
        return "Anthropic Claude";
    }

    @Override
    public String getModel() {
        return model;
    }
}