package config;

public enum LLMProvider {
    OLLAMA("Ollama", "localhost:11434"),
    BEDROCK("AWS Bedrock", "bedrock-runtime.us-east-1.amazonaws.com"),
    CODEX("OpenAI Codex", "localhost");

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
