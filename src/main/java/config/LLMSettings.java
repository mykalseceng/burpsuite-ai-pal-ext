package config;

public class LLMSettings {
    private LLMProvider activeProvider;
    private String openaiApiKey;
    private String openaiModel;
    private String geminiApiKey;
    private String geminiModel;
    private String claudeApiKey;
    private String claudeModel;

    // Latest models as of December 2025
    public static final String[] OPENAI_MODELS = {
        "gpt-5.2",              // GPT-5.2 Thinking - best for coding and planning
        "gpt-5.2-chat-latest",  // GPT-5.2 Instant - faster for writing
        "gpt-5.2-pro",          // GPT-5.2 Pro - most accurate
        "gpt-4o",               // GPT-4o - reliable fallback
        "gpt-4o-mini"           // GPT-4o Mini - cost-effective
    };

    public static final String[] GEMINI_MODELS = {
        "gemini-3-flash",       // Gemini 3 Flash - frontier intelligence, fast
        "gemini-3-pro",         // Gemini 3 Pro - most powerful
        "gemini-2.5-flash",     // Gemini 2.5 Flash - previous gen
        "gemini-2.5-pro"        // Gemini 2.5 Pro - previous gen
    };

    public static final String[] CLAUDE_MODELS = {
        "claude-opus-4-5-20251101",       // Claude Opus 4.5 - most capable
        "claude-sonnet-4-5-20250929",     // Claude Sonnet 4.5 - balanced
        "claude-haiku-4-5-20251001"       // Claude Haiku 4.5 - fast, cost-efficient
    };

    public LLMSettings() {
        this.activeProvider = LLMProvider.OPENAI;
        this.openaiApiKey = "";
        this.openaiModel = "gpt-5.2";
        this.geminiApiKey = "";
        this.geminiModel = "gemini-3-flash";
        this.claudeApiKey = "";
        this.claudeModel = "claude-sonnet-4-5-20250929";
    }

    public LLMProvider getActiveProvider() {
        return activeProvider;
    }

    public void setActiveProvider(LLMProvider activeProvider) {
        this.activeProvider = activeProvider;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getOpenaiModel() {
        return openaiModel;
    }

    public void setOpenaiModel(String openaiModel) {
        this.openaiModel = openaiModel;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public String getClaudeApiKey() {
        return claudeApiKey;
    }

    public void setClaudeApiKey(String claudeApiKey) {
        this.claudeApiKey = claudeApiKey;
    }

    public String getClaudeModel() {
        return claudeModel;
    }

    public void setClaudeModel(String claudeModel) {
        this.claudeModel = claudeModel;
    }

    public String getApiKey(LLMProvider provider) {
        return switch (provider) {
            case OPENAI -> openaiApiKey;
            case GEMINI -> geminiApiKey;
            case CLAUDE -> claudeApiKey;
        };
    }

    public String getModel(LLMProvider provider) {
        return switch (provider) {
            case OPENAI -> openaiModel;
            case GEMINI -> geminiModel;
            case CLAUDE -> claudeModel;
        };
    }

    public void setApiKey(LLMProvider provider, String apiKey) {
        switch (provider) {
            case OPENAI -> openaiApiKey = apiKey;
            case GEMINI -> geminiApiKey = apiKey;
            case CLAUDE -> claudeApiKey = apiKey;
        }
    }

    public void setModel(LLMProvider provider, String model) {
        switch (provider) {
            case OPENAI -> openaiModel = model;
            case GEMINI -> geminiModel = model;
            case CLAUDE -> claudeModel = model;
        }
    }

    public static String[] getModelsForProvider(LLMProvider provider) {
        return switch (provider) {
            case OPENAI -> OPENAI_MODELS;
            case GEMINI -> GEMINI_MODELS;
            case CLAUDE -> CLAUDE_MODELS;
        };
    }
}