package config;

public enum LLMProvider {
    OPENAI("OpenAI", "api.openai.com"),
    GEMINI("Google Gemini", "generativelanguage.googleapis.com"),
    CLAUDE("Anthropic Claude", "api.anthropic.com");

    private final String displayName;
    private final String apiHost;

    LLMProvider(String displayName, String apiHost) {
        this.displayName = displayName;
        this.apiHost = apiHost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getApiHost() {
        return apiHost;
    }

    @Override
    public String toString() {
        return displayName;
    }
}