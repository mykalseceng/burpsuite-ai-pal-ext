package config;

import burp.api.montoya.persistence.Preferences;

public class SettingsManager {
    private static final String KEY_ACTIVE_PROVIDER = "llm.activeProvider";
    private static final String KEY_OPENAI_API_KEY = "llm.openai.apiKey";
    private static final String KEY_OPENAI_MODEL = "llm.openai.model";
    private static final String KEY_GEMINI_API_KEY = "llm.gemini.apiKey";
    private static final String KEY_GEMINI_MODEL = "llm.gemini.model";
    private static final String KEY_CLAUDE_API_KEY = "llm.claude.apiKey";
    private static final String KEY_CLAUDE_MODEL = "llm.claude.model";

    private final Preferences preferences;
    private LLMSettings settings;

    public SettingsManager(Preferences preferences) {
        this.preferences = preferences;
        this.settings = loadSettings();
    }

    private LLMSettings loadSettings() {
        LLMSettings s = new LLMSettings();

        String providerName = preferences.getString(KEY_ACTIVE_PROVIDER);
        if (providerName != null) {
            try {
                s.setActiveProvider(LLMProvider.valueOf(providerName));
            } catch (IllegalArgumentException ignored) {
            }
        }

        String openaiKey = preferences.getString(KEY_OPENAI_API_KEY);
        if (openaiKey != null) s.setOpenaiApiKey(openaiKey);

        String openaiModel = preferences.getString(KEY_OPENAI_MODEL);
        if (openaiModel != null) s.setOpenaiModel(openaiModel);

        String geminiKey = preferences.getString(KEY_GEMINI_API_KEY);
        if (geminiKey != null) s.setGeminiApiKey(geminiKey);

        String geminiModel = preferences.getString(KEY_GEMINI_MODEL);
        if (geminiModel != null) s.setGeminiModel(geminiModel);

        String claudeKey = preferences.getString(KEY_CLAUDE_API_KEY);
        if (claudeKey != null) s.setClaudeApiKey(claudeKey);

        String claudeModel = preferences.getString(KEY_CLAUDE_MODEL);
        if (claudeModel != null) s.setClaudeModel(claudeModel);

        return s;
    }

    public void saveSettings() {
        preferences.setString(KEY_ACTIVE_PROVIDER, settings.getActiveProvider().name());
        preferences.setString(KEY_OPENAI_API_KEY, settings.getOpenaiApiKey());
        preferences.setString(KEY_OPENAI_MODEL, settings.getOpenaiModel());
        preferences.setString(KEY_GEMINI_API_KEY, settings.getGeminiApiKey());
        preferences.setString(KEY_GEMINI_MODEL, settings.getGeminiModel());
        preferences.setString(KEY_CLAUDE_API_KEY, settings.getClaudeApiKey());
        preferences.setString(KEY_CLAUDE_MODEL, settings.getClaudeModel());
    }

    public LLMSettings getSettings() {
        return settings;
    }

    public LLMProvider getActiveProvider() {
        return settings.getActiveProvider();
    }

    public void setActiveProvider(LLMProvider provider) {
        settings.setActiveProvider(provider);
        saveSettings();
    }

    public String getApiKey(LLMProvider provider) {
        return settings.getApiKey(provider);
    }

    public void setApiKey(LLMProvider provider, String apiKey) {
        settings.setApiKey(provider, apiKey);
        saveSettings();
    }

    public String getModel(LLMProvider provider) {
        return settings.getModel(provider);
    }

    public void setModel(LLMProvider provider, String model) {
        settings.setModel(provider, model);
        saveSettings();
    }

    public String getCurrentApiKey() {
        return getApiKey(getActiveProvider());
    }

    public String getCurrentModel() {
        return getModel(getActiveProvider());
    }
}