package config;

import burp.api.montoya.persistence.Preferences;

public class SettingsManager {
    private static final String KEY_ACTIVE_PROVIDER = "llm.activeProvider";

    // Ollama settings keys
    private static final String KEY_OLLAMA_BASE_URL = "llm.ollama.baseUrl";
    private static final String KEY_OLLAMA_MODEL = "llm.ollama.model";

    // Claude Code settings keys
    private static final String KEY_CLAUDE_CODE_PATH = "llm.claudecode.path";
    private static final String KEY_CLAUDE_CODE_MODEL = "llm.claudecode.model";

    // Bedrock settings keys
    private static final String KEY_BEDROCK_ACCESS_KEY = "llm.bedrock.accessKey";
    private static final String KEY_BEDROCK_SECRET_KEY = "llm.bedrock.secretKey";
    private static final String KEY_BEDROCK_SESSION_TOKEN = "llm.bedrock.sessionToken";
    private static final String KEY_BEDROCK_REGION = "llm.bedrock.region";
    private static final String KEY_BEDROCK_MODEL = "llm.bedrock.model";

    // Codex settings keys
    private static final String KEY_CODEX_PATH = "llm.codex.path";
    private static final String KEY_CODEX_MODEL = "llm.codex.model";

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
                // Default to OLLAMA if invalid
                s.setActiveProvider(LLMProvider.OLLAMA);
            }
        }

        // Load Ollama settings
        String ollamaBaseUrl = preferences.getString(KEY_OLLAMA_BASE_URL);
        if (ollamaBaseUrl != null) s.setOllamaBaseUrl(ollamaBaseUrl);

        String ollamaModel = preferences.getString(KEY_OLLAMA_MODEL);
        if (ollamaModel != null) s.setOllamaModel(ollamaModel);

        // Load Claude Code settings
        String claudeCodePath = preferences.getString(KEY_CLAUDE_CODE_PATH);
        if (claudeCodePath != null) s.setClaudeCodePath(claudeCodePath);

        String claudeCodeModel = preferences.getString(KEY_CLAUDE_CODE_MODEL);
        if (claudeCodeModel != null) s.setClaudeCodeModel(claudeCodeModel);

        // Load Bedrock settings
        String bedrockAccessKey = preferences.getString(KEY_BEDROCK_ACCESS_KEY);
        if (bedrockAccessKey != null) s.setBedrockAccessKey(bedrockAccessKey);

        String bedrockSecretKey = preferences.getString(KEY_BEDROCK_SECRET_KEY);
        if (bedrockSecretKey != null) s.setBedrockSecretKey(bedrockSecretKey);

        String bedrockSessionToken = preferences.getString(KEY_BEDROCK_SESSION_TOKEN);
        if (bedrockSessionToken != null) s.setBedrockSessionToken(bedrockSessionToken);

        String bedrockRegion = preferences.getString(KEY_BEDROCK_REGION);
        if (bedrockRegion != null) s.setBedrockRegion(bedrockRegion);

        String bedrockModel = preferences.getString(KEY_BEDROCK_MODEL);
        if (bedrockModel != null) s.setBedrockModel(bedrockModel);

        // Load Codex settings
        String codexPath = preferences.getString(KEY_CODEX_PATH);
        if (codexPath != null) s.setCodexPath(codexPath);

        String codexModel = preferences.getString(KEY_CODEX_MODEL);
        if (codexModel != null) s.setCodexModel(codexModel);

        return s;
    }

    public void saveSettings() {
        preferences.setString(KEY_ACTIVE_PROVIDER, settings.getActiveProvider().name());

        // Save Ollama settings
        preferences.setString(KEY_OLLAMA_BASE_URL, settings.getOllamaBaseUrl());
        preferences.setString(KEY_OLLAMA_MODEL, settings.getOllamaModel());

        // Save Claude Code settings
        preferences.setString(KEY_CLAUDE_CODE_PATH, settings.getClaudeCodePath());
        preferences.setString(KEY_CLAUDE_CODE_MODEL, settings.getClaudeCodeModel());

        // Save Bedrock settings
        // WARNING: AWS credentials are stored as plain text in Burp preferences.
        // For production use, consider using environment variables or AWS credentials file instead.
        preferences.setString(KEY_BEDROCK_ACCESS_KEY, settings.getBedrockAccessKey());
        preferences.setString(KEY_BEDROCK_SECRET_KEY, settings.getBedrockSecretKey());
        preferences.setString(KEY_BEDROCK_SESSION_TOKEN, settings.getBedrockSessionToken());
        preferences.setString(KEY_BEDROCK_REGION, settings.getBedrockRegion());
        preferences.setString(KEY_BEDROCK_MODEL, settings.getBedrockModel());

        // Save Codex settings
        preferences.setString(KEY_CODEX_PATH, settings.getCodexPath());
        preferences.setString(KEY_CODEX_MODEL, settings.getCodexModel());
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

    public String getModel(LLMProvider provider) {
        return settings.getModel(provider);
    }

    public void setModel(LLMProvider provider, String model) {
        settings.setModel(provider, model);
        saveSettings();
    }

    public String getCurrentModel() {
        return getModel(getActiveProvider());
    }

    // Ollama-specific accessors
    public String getOllamaBaseUrl() {
        return settings.getOllamaBaseUrl();
    }

    public void setOllamaBaseUrl(String baseUrl) {
        settings.setOllamaBaseUrl(baseUrl);
        saveSettings();
    }

    // Claude Code-specific accessors
    public String getClaudeCodePath() {
        return settings.getClaudeCodePath();
    }

    public void setClaudeCodePath(String path) {
        settings.setClaudeCodePath(path);
        saveSettings();
    }

    // Bedrock-specific accessors
    public String getBedrockAccessKey() {
        return settings.getBedrockAccessKey();
    }

    public void setBedrockAccessKey(String accessKey) {
        settings.setBedrockAccessKey(accessKey);
        saveSettings();
    }

    public String getBedrockSecretKey() {
        return settings.getBedrockSecretKey();
    }

    public void setBedrockSecretKey(String secretKey) {
        settings.setBedrockSecretKey(secretKey);
        saveSettings();
    }

    public String getBedrockSessionToken() {
        return settings.getBedrockSessionToken();
    }

    public void setBedrockSessionToken(String sessionToken) {
        settings.setBedrockSessionToken(sessionToken);
        saveSettings();
    }

    public String getBedrockRegion() {
        return settings.getBedrockRegion();
    }

    public void setBedrockRegion(String region) {
        settings.setBedrockRegion(region);
        saveSettings();
    }

    // Codex-specific accessors
    public String getCodexPath() {
        return settings.getCodexPath();
    }

    public void setCodexPath(String path) {
        settings.setCodexPath(path);
        saveSettings();
    }

    /**
     * Check if the provider has valid configuration.
     */
    public boolean hasValidConfig() {
        return settings.hasValidConfig(getActiveProvider());
    }

    public boolean hasValidConfig(LLMProvider provider) {
        return settings.hasValidConfig(provider);
    }
}
