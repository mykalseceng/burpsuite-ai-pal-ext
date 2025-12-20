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

public class GeminiClient implements LLMClient {
    private static final String API_HOST = "generativelanguage.googleapis.com";

    private final Http http;
    private final Logging logging;
    private final String apiKey;
    private final String model;

    public GeminiClient(Http http, Logging logging, String apiKey, String model) {
        this.http = http;
        this.logging = logging;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public LLMResponse complete(String prompt, String systemPrompt) {
        try {
            JsonObject requestBody = new JsonObject();

            // Add system instruction if provided
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemInstruction = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", Utf16Sanitizer.sanitize(systemPrompt));
                parts.add(textPart);
                systemInstruction.add("parts", parts);
                requestBody.add("system_instruction", systemInstruction);
            }

            // Add user content
            JsonArray contents = new JsonArray();
            JsonObject userContent = new JsonObject();
            userContent.addProperty("role", "user");
            JsonArray userParts = new JsonArray();
            JsonObject userText = new JsonObject();
            userText.addProperty("text", Utf16Sanitizer.sanitize(prompt));
            userParts.add(userText);
            userContent.add("parts", userParts);
            contents.add(userContent);
            requestBody.add("contents", contents);

            return sendRequest(requestBody);
        } catch (Exception e) {
            logging.logToError("Gemini API error: " + e.getMessage());
            return LLMResponse.error("Gemini API error: " + e.getMessage());
        }
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, String newMessage) {
        try {
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();

            // Find system message if any and set as system_instruction
            for (ChatMessage msg : messages) {
                if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                    JsonObject systemInstruction = new JsonObject();
                    JsonArray parts = new JsonArray();
                    JsonObject textPart = new JsonObject();
                    textPart.addProperty("text", Utf16Sanitizer.sanitize(msg.getFullContent()));
                    parts.add(textPart);
                    systemInstruction.add("parts", parts);
                    requestBody.add("system_instruction", systemInstruction);
                    break;
                }
            }

            // Add conversation history
            for (ChatMessage msg : messages) {
                if (msg.getRole() == ChatMessage.Role.SYSTEM) {
                    continue; // Skip system messages, handled above
                }

                JsonObject content = new JsonObject();
                content.addProperty("role", msg.getRole() == ChatMessage.Role.USER ? "user" : "model");
                JsonArray parts = new JsonArray();
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", Utf16Sanitizer.sanitize(msg.getFullContent()));
                parts.add(textPart);
                content.add("parts", parts);
                contents.add(content);
            }

            // Add new user message
            JsonObject userContent = new JsonObject();
            userContent.addProperty("role", "user");
            JsonArray userParts = new JsonArray();
            JsonObject userText = new JsonObject();
            userText.addProperty("text", Utf16Sanitizer.sanitize(newMessage));
            userParts.add(userText);
            userContent.add("parts", userParts);
            contents.add(userContent);

            requestBody.add("contents", contents);

            return sendRequest(requestBody);
        } catch (Exception e) {
            logging.logToError("Gemini chat error: " + e.getMessage());
            return LLMResponse.error("Gemini chat error: " + e.getMessage());
        }
    }

    private LLMResponse sendRequest(JsonObject requestBody) {
        String path = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        String body = Utf16Sanitizer.sanitize(requestBody.toString());
        ByteArray bodyBytes = ByteArray.byteArray(body.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.httpRequest()
                .withService(HttpService.httpService(API_HOST, 443, true))
                .withMethod("POST")
                .withPath(path)
                .withHeader("Host", API_HOST)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(bodyBytes);

        HttpRequestResponse response = http.sendRequest(request);

        if (response.response() == null) {
            return LLMResponse.error("No response from Gemini API");
        }

        int statusCode = response.response().statusCode();
        String responseBody = response.response().bodyToString();

        if (statusCode != 200) {
            return LLMResponse.error("Gemini API error (HTTP " + statusCode + "): " + responseBody);
        }

        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && !parts.isEmpty()) {
                    String text = parts.get(0).getAsJsonObject().get("text").getAsString();

                    int tokensUsed = 0;
                    if (jsonResponse.has("usageMetadata")) {
                        JsonObject usage = jsonResponse.getAsJsonObject("usageMetadata");
                        if (usage.has("totalTokenCount")) {
                            tokensUsed = usage.get("totalTokenCount").getAsInt();
                        }
                    }

                    return LLMResponse.success(text, tokensUsed);
                }
            }
            return LLMResponse.error("No response content from Gemini");
        } catch (Exception e) {
            logging.logToError("Failed to parse Gemini response: " + e.getMessage());
            return LLMResponse.error("Failed to parse Gemini response: " + e.getMessage());
        }
    }

    @Override
    public boolean testConnection() {
        LLMResponse response = complete("Say 'OK' if you can read this.", null);
        return response.isSuccess();
    }

    @Override
    public String getProviderName() {
        return "Google Gemini";
    }

    @Override
    public String getModel() {
        return model;
    }
}