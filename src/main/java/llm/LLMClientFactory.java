package llm;

import burp.api.montoya.http.Http;
import burp.api.montoya.logging.Logging;
import config.LLMProvider;
import config.SettingsManager;
import llm.impl.ClaudeClient;
import llm.impl.GeminiClient;
import llm.impl.OpenAIClient;

public class LLMClientFactory {
    private final Http http;
    private final Logging logging;
    private final SettingsManager settingsManager;

    public LLMClientFactory(Http http, Logging logging, SettingsManager settingsManager) {
        this.http = http;
        this.logging = logging;
        this.settingsManager = settingsManager;
    }

    public LLMClient createClient() {
        LLMProvider provider = settingsManager.getActiveProvider();
        return createClient(provider);
    }

    public LLMClient createClient(LLMProvider provider) {
        String apiKey = settingsManager.getApiKey(provider);
        String model = settingsManager.getModel(provider);

        return switch (provider) {
            case OPENAI -> new OpenAIClient(http, logging, apiKey, model);
            case GEMINI -> new GeminiClient(http, logging, apiKey, model);
            case CLAUDE -> new ClaudeClient(http, logging, apiKey, model);
        };
    }

    public boolean hasValidApiKey() {
        String apiKey = settingsManager.getCurrentApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public boolean hasValidApiKey(LLMProvider provider) {
        String apiKey = settingsManager.getApiKey(provider);
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}