package ui.tasks;

import java.time.Instant;
import java.util.UUID;

public class AITask {
    private final String id;
    private final Instant createdAt;
    private final String title;
    private final String prompt;
    private final String requestText;
    private final String responseText;
    private final String notesText;

    private volatile AITaskStatus status;
    private volatile String result;
    private volatile String error;
    private volatile Integer tokensUsed;

    public AITask(String title, String prompt, String requestText, String responseText, String notesText) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.title = title;
        this.prompt = prompt;
        this.requestText = requestText;
        this.responseText = responseText;
        this.notesText = notesText;
        this.status = AITaskStatus.PENDING;
    }

    public String id() {
        return id;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String title() {
        return title;
    }

    public String prompt() {
        return prompt;
    }

    public String requestText() {
        return requestText;
    }

    public String responseText() {
        return responseText;
    }

    public String notesText() {
        return notesText;
    }

    public AITaskStatus status() {
        return status;
    }

    public String result() {
        return result;
    }

    public String error() {
        return error;
    }

    public Integer tokensUsed() {
        return tokensUsed;
    }

    public void markRunning() {
        this.status = AITaskStatus.RUNNING;
        this.error = null;
    }

    public void markCompleted(String result, Integer tokensUsed) {
        this.status = AITaskStatus.COMPLETED;
        this.result = result;
        this.tokensUsed = tokensUsed;
        this.error = null;
    }

    public void markFailed(String error) {
        this.status = AITaskStatus.FAILED;
        this.error = error;
    }
}


