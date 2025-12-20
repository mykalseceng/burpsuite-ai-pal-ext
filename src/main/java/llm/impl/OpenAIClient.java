package llm.impl;

import burp.api.montoya.http.Http;
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

public class OpenAIClient implements LLMClient {
    private static final String API_HOST = "api.openai.com";
    private static final String API_PATH = "/v1/chat/completions";

    private final Http http;
    private final Logging logging;
    private final String apiKey;
    private final String model;

    public OpenAIClient(Http http, Logging logging, String apiKey, String model) {
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

            JsonArray messages = new JsonArray();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", Utf16Sanitizer.sanitize(systemPrompt));
                messages.add(systemMsg);
            }

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", Utf16Sanitizer.sanitize(prompt));
            messages.add(userMsg);

            requestBody.add("messages", messages);

            return sendRequest(requestBody);
        } catch (Exception e) {
            logging.logToError("OpenAI API error: " + e.getMessage());
            return LLMResponse.error("OpenAI API error: " + e.getMessage());
        }
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, String newMessage) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);

            JsonArray messagesArray = new JsonArray();

            for (ChatMessage msg : messages) {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", msg.getRole().getApiValue());
                msgObj.addProperty("content", Utf16Sanitizer.sanitize(msg.getFullContent()));
                messagesArray.add(msgObj);
            }

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", Utf16Sanitizer.sanitize(newMessage));
            messagesArray.add(userMsg);

            requestBody.add("messages", messagesArray);

            return sendRequest(requestBody);
        } catch (Exception e) {
            logging.logToError("OpenAI chat error: " + e.getMessage());
            return LLMResponse.error("OpenAI chat error: " + e.getMessage());
        }
    }

    private LLMResponse sendRequest(JsonObject requestBody) {
        String body = Utf16Sanitizer.sanitize(requestBody.toString());
        ByteArray bodyBytes = ByteArray.byteArray(body.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.httpRequest()
                .withService(burp.api.montoya.http.HttpService.httpService(API_HOST, 443, true))
                .withMethod("POST")
                .withPath(API_PATH)
                .withHeader("Host", API_HOST)
                .withHeader("Authorization", "Bearer " + apiKey)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(bodyBytes);

        HttpRequestResponse response = http.sendRequest(request);

        if (response.response() == null) {
            return LLMResponse.error("No response from OpenAI API");
        }

        int statusCode = response.response().statusCode();
        String responseBody = response.response().bodyToString();

        if (statusCode != 200) {
            return LLMResponse.error("OpenAI API error (HTTP " + statusCode + "): " + responseBody);
        }

        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = jsonResponse.getAsJsonArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                String content = message.get("content").getAsString();

                int tokensUsed = 0;
                if (jsonResponse.has("usage")) {
                    tokensUsed = jsonResponse.getAsJsonObject("usage").get("total_tokens").getAsInt();
                }

                return LLMResponse.success(content, tokensUsed);
            }
            return LLMResponse.error("No response content from OpenAI");
        } catch (Exception e) {
            logging.logToError("Failed to parse OpenAI response: " + e.getMessage());
            return LLMResponse.error("Failed to parse OpenAI response: " + e.getMessage());
        }
    }

    @Override
    public boolean testConnection() {
        LLMResponse response = complete("Say 'OK' if you can read this.", null);
        return response.isSuccess();
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public String getModel() {
        return model;
    }
}