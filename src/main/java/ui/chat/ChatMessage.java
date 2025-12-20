package ui.chat;

import java.time.Instant;

public class ChatMessage {
    public enum Role {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system");

        private final String apiValue;

        Role(String apiValue) {
            this.apiValue = apiValue;
        }

        public String getApiValue() {
            return apiValue;
        }
    }

    private final Role role;
    private final String content;
    private final Instant timestamp;
    private final String attachedRequest;

    public ChatMessage(Role role, String content) {
        this(role, content, Instant.now(), null);
    }

    public ChatMessage(Role role, String content, String attachedRequest) {
        this(role, content, Instant.now(), attachedRequest);
    }

    public ChatMessage(Role role, String content, Instant timestamp, String attachedRequest) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.attachedRequest = attachedRequest;
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getAttachedRequest() {
        return attachedRequest;
    }

    public boolean hasAttachedRequest() {
        return attachedRequest != null && !attachedRequest.isEmpty();
    }

    public String getFullContent() {
        if (hasAttachedRequest()) {
            return content + "\n\n--- Attached HTTP Request/Response ---\n" + attachedRequest;
        }
        return content;
    }
}