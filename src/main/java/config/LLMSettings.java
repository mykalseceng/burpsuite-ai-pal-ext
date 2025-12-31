package config;

public class LLMSettings {
    private LLMProvider activeProvider;

    // Ollama settings
    private String ollamaBaseUrl;
    private String ollamaModel;

    // Bedrock settings
    private String bedrockAccessKey;
    private String bedrockSecretKey;
    private String bedrockSessionToken;
    private String bedrockRegion;
    private String bedrockModel;

    // AWS Bedrock models - Anthropic Claude global inference profiles
    public static final String[] BEDROCK_MODELS = {
        "global.anthropic.claude-sonnet-4-5-20250929-v1:0",
        "global.anthropic.claude-sonnet-4-20250514-v1:0",
        "global.anthropic.claude-haiku-4-5-20251001-v1:0",
        "global.anthropic.claude-opus-4-5-20251101-v1:0"
    };

    // AWS Bedrock regions that support Bedrock
    public static final String[] BEDROCK_REGIONS = {
        "us-east-1",
        "us-west-2",
        "eu-west-1",
        "eu-central-1",
        "ap-southeast-1",
        "ap-northeast-1"
    };

    public LLMSettings() {
        this.activeProvider = LLMProvider.OLLAMA;
        this.ollamaBaseUrl = "http://localhost:11434";
        this.ollamaModel = "llama3.2";
        this.bedrockAccessKey = "";
        this.bedrockSecretKey = "";
        this.bedrockSessionToken = "";
        this.bedrockRegion = "us-east-1";
        this.bedrockModel = "global.anthropic.claude-sonnet-4-5-20250929-v1:0";
    }

    public LLMProvider getActiveProvider() {
        return activeProvider;
    }

    public void setActiveProvider(LLMProvider activeProvider) {
        this.activeProvider = activeProvider;
    }

    // Ollama getters/setters
    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public void setOllamaBaseUrl(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public String getOllamaModel() {
        return ollamaModel;
    }

    public void setOllamaModel(String ollamaModel) {
        this.ollamaModel = ollamaModel;
    }

    // Bedrock getters/setters
    public String getBedrockAccessKey() {
        return bedrockAccessKey;
    }

    public void setBedrockAccessKey(String bedrockAccessKey) {
        this.bedrockAccessKey = bedrockAccessKey;
    }

    public String getBedrockSecretKey() {
        return bedrockSecretKey;
    }

    public void setBedrockSecretKey(String bedrockSecretKey) {
        this.bedrockSecretKey = bedrockSecretKey;
    }

    public String getBedrockSessionToken() {
        return bedrockSessionToken;
    }

    public void setBedrockSessionToken(String bedrockSessionToken) {
        this.bedrockSessionToken = bedrockSessionToken;
    }

    public String getBedrockRegion() {
        return bedrockRegion;
    }

    public void setBedrockRegion(String bedrockRegion) {
        this.bedrockRegion = bedrockRegion;
    }

    public String getBedrockModel() {
        return bedrockModel;
    }

    public void setBedrockModel(String bedrockModel) {
        this.bedrockModel = bedrockModel;
    }

    // Generic accessors for provider abstraction
    public String getModel(LLMProvider provider) {
        return switch (provider) {
            case OLLAMA -> ollamaModel;
            case BEDROCK -> bedrockModel;
        };
    }

    public void setModel(LLMProvider provider, String model) {
        switch (provider) {
            case OLLAMA -> ollamaModel = model;
            case BEDROCK -> bedrockModel = model;
        }
    }

    public static String[] getModelsForProvider(LLMProvider provider) {
        return switch (provider) {
            case OLLAMA -> new String[0]; // Ollama models are user-specified
            case BEDROCK -> BEDROCK_MODELS;
        };
    }

    /**
     * Check if the provider has valid configuration.
     * For Ollama: needs base URL
     * For Bedrock: needs either env credentials or explicit keys
     */
    public boolean hasValidConfig(LLMProvider provider) {
        return switch (provider) {
            case OLLAMA -> ollamaBaseUrl != null && !ollamaBaseUrl.trim().isEmpty();
            case BEDROCK -> hasBedrockCredentials();
        };
    }

    private boolean hasBedrockCredentials() {
        // Check for explicit credentials in settings
        boolean hasExplicit = bedrockAccessKey != null && !bedrockAccessKey.trim().isEmpty()
                && bedrockSecretKey != null && !bedrockSecretKey.trim().isEmpty();

        // Check for environment credentials
        boolean hasEnv = System.getenv("AWS_ACCESS_KEY_ID") != null
                && System.getenv("AWS_SECRET_ACCESS_KEY") != null;

        // Check for AWS credentials file (must actually exist)
        boolean hasCredentialsFile = false;
        String home = System.getProperty("user.home");
        if (home != null) {
            java.io.File credFile = new java.io.File(home, ".aws/credentials");
            hasCredentialsFile = credFile.exists() && credFile.canRead();
        }

        // Check for AWS_PROFILE with credentials file
        boolean hasProfile = System.getenv("AWS_PROFILE") != null && hasCredentialsFile;

        return hasExplicit || hasEnv || hasCredentialsFile || hasProfile;
    }
}
