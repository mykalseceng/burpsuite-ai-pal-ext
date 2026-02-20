package llm;

import config.LLMProvider;
import config.SettingsManager;
import llm.impl.BedrockClient;
import llm.impl.ClaudeCodeClient;
import llm.impl.CodexClient;
import llm.impl.OllamaClient;

public class LLMClientFactory {
    private final SettingsManager settingsManager;

    public LLMClientFactory(SettingsManager settingsManager) {
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
                    settingsManager.getOllamaBaseUrl(),
                    model
            );
            case BEDROCK -> new BedrockClient(
                    settingsManager.getBedrockAccessKey(),
                    settingsManager.getBedrockSecretKey(),
                    settingsManager.getBedrockSessionToken(),
                    settingsManager.getBedrockRegion(),
                    model
            );
            case CLAUDE_CODE -> new ClaudeCodeClient(
                    settingsManager.getClaudeCodePath(),
                    model
            );
            case CODEX -> new CodexClient(
                    settingsManager.getCodexPath(),
                    model
            );
        };
    }

    public boolean hasValidConfig() {
        return settingsManager.hasValidConfig();
    }

    public boolean hasValidConfig(LLMProvider provider) {
        return settingsManager.hasValidConfig(provider);
    }
}
