package llm;

import ui.chat.ChatMessage;

import java.util.List;

public interface LLMClient {

    /**
     * Callback interface for streaming responses.
     */
    interface StreamCallback {
        /**
         * Called when a new chunk of content is received.
         * @param chunk The new content chunk
         */
        void onChunk(String chunk);

        /**
         * Called when streaming is complete.
         * @param totalTokens The total tokens used
         */
        void onComplete(int totalTokens);

        /**
         * Called when an error occurs.
         * @param error The error message
         */
        void onError(String error);

        /**
         * Check if streaming should be cancelled.
         * @return true if streaming should stop
         */
        default boolean isCancelled() {
            return false;
        }
    }

    /**
     * Send a single prompt to the LLM and get a response.
     *
     * @param prompt       The user prompt
     * @param systemPrompt Optional system prompt for context (can be null)
     * @return The LLM response
     */
    LLMResponse complete(String prompt, String systemPrompt);

    /**
     * Send a prompt with conversation history for multi-turn chat.
     *
     * @param messages   List of previous messages in the conversation
     * @param newMessage The new user message
     * @return The LLM response
     */
    LLMResponse chat(List<ChatMessage> messages, String newMessage);

    /**
     * Send a prompt with conversation history for multi-turn chat with streaming.
     *
     * @param messages   List of previous messages in the conversation
     * @param newMessage The new user message
     * @param callback   Callback for streaming chunks
     */
    default void chatStreaming(List<ChatMessage> messages, String newMessage, StreamCallback callback) {
        // Default implementation falls back to non-streaming
        LLMResponse response = chat(messages, newMessage);
        if (response.isSuccess()) {
            callback.onChunk(response.getContent());
            callback.onComplete(response.getTokensUsed());
        } else {
            callback.onError(response.getErrorMessage());
        }
    }

    /**
     * Check if this client supports streaming.
     * @return true if streaming is supported
     */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * Test if the API connection is valid.
     *
     * @return true if the connection is valid
     */
    boolean testConnection();

    /**
     * Get the provider name for display.
     *
     * @return The provider display name
     */
    String getProviderName();

    /**
     * Get the current model being used.
     *
     * @return The model identifier
     */
    String getModel();
}