package llm;

import burp.api.montoya.http.Http;
import burp.api.montoya.logging.Logging;
import config.LLMProvider;
import config.SettingsManager;
import llm.impl.BedrockClient;
import llm.impl.OllamaClient;

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
        String model = settingsManager.getModel(provider);

        return switch (provider) {
            case OLLAMA -> new OllamaClient(
                    http,
                    logging,
                    settingsManager.getOllamaBaseUrl(),
                    model
            );
            case BEDROCK -> new BedrockClient(
                    http,
                    logging,
                    settingsManager.getBedrockAccessKey(),
                    settingsManager.getBedrockSecretKey(),
                    settingsManager.getBedrockSessionToken(),
                    settingsManager.getBedrockRegion(),
                    model
            );
        };
    }

    /**
     * Check if the current provider has valid configuration.
     */
    public boolean hasValidConfig() {
        return settingsManager.hasValidConfig();
    }

    /**
     * Check if a specific provider has valid configuration.
     */
    public boolean hasValidConfig(LLMProvider provider) {
        return settingsManager.hasValidConfig(provider);
    }
}
