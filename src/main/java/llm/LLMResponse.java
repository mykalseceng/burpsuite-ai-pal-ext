package llm;

public class LLMResponse {
    private final boolean success;
    private final String content;
    private final String errorMessage;
    private final int tokensUsed;

    private LLMResponse(boolean success, String content, String errorMessage, int tokensUsed) {
        this.success = success;
        this.content = content;
        this.errorMessage = errorMessage;
        this.tokensUsed = tokensUsed;
    }

    public static LLMResponse success(String content) {
        return new LLMResponse(true, content, null, 0);
    }

    public static LLMResponse success(String content, int tokensUsed) {
        return new LLMResponse(true, content, null, tokensUsed);
    }

    public static LLMResponse error(String errorMessage) {
        return new LLMResponse(false, null, errorMessage, 0);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    @Override
    public String toString() {
        if (success) {
            return "LLMResponse{success=true, content='" +
                   (content.length() > 100 ? content.substring(0, 100) + "..." : content) + "'}";
        } else {
            return "LLMResponse{success=false, error='" + errorMessage + "'}";
        }
    }
}