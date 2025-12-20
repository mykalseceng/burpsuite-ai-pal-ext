package ui.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConversationHistory {
    private static final int MAX_HISTORY_SIZE = 100;

    private final List<ChatMessage> messages;
    private final List<ConversationListener> listeners;

    public interface ConversationListener {
        void onMessageAdded(ChatMessage message);
        void onHistoryCleared();
    }

    public ConversationHistory() {
        this.messages = new CopyOnWriteArrayList<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);

        // Trim history if too large
        while (messages.size() > MAX_HISTORY_SIZE) {
            messages.remove(0);
        }

        // Notify listeners
        for (ConversationListener listener : listeners) {
            listener.onMessageAdded(message);
        }
    }

    public void addUserMessage(String content) {
        addMessage(new ChatMessage(ChatMessage.Role.USER, content));
    }

    public void addUserMessage(String content, String attachedRequest) {
        addMessage(new ChatMessage(ChatMessage.Role.USER, content, attachedRequest));
    }

    public void addAssistantMessage(String content) {
        addMessage(new ChatMessage(ChatMessage.Role.ASSISTANT, content));
    }

    public void addSystemMessage(String content) {
        addMessage(new ChatMessage(ChatMessage.Role.SYSTEM, content));
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public void clear() {
        messages.clear();
        for (ConversationListener listener : listeners) {
            listener.onHistoryCleared();
        }
    }

    public int size() {
        return messages.size();
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public void addListener(ConversationListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ConversationListener listener) {
        listeners.remove(listener);
    }
}