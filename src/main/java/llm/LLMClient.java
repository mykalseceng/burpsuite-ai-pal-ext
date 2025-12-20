package llm;

import ui.chat.ChatMessage;

import java.util.List;

public interface LLMClient {

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