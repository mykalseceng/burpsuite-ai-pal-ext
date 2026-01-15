package llm;

import java.util.List;

import ui.chat.ChatMessage;

public interface LLMClient {

    interface StreamCallback {
        void onChunk(String chunk);
        void onComplete(int totalTokens);
        void onError(String error);
        default boolean isCancelled() {
            return false;
        }
    }

    LLMResponse complete(String prompt, String systemPrompt);

    LLMResponse chat(List<ChatMessage> messages, String newMessage);

    default void chatStreaming(List<ChatMessage> messages, String newMessage, StreamCallback callback) {
        LLMResponse response = chat(messages, newMessage);
        if (response.isSuccess()) {
            callback.onChunk(response.getContent());
            callback.onComplete(response.getTokensUsed());
        } else {
            callback.onError(response.getErrorMessage());
        }
    }

    default boolean supportsStreaming() {
        return false;
    }

    boolean testConnection();

    String getProviderName();

    String getModel();
}
