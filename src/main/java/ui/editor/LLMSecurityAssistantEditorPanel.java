package ui.editor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import config.SettingsManager;
import llm.LLMClient;
import llm.LLMClientFactory;
import llm.LLMResponse;
import llm.prompts.PromptTemplates;
import ui.tasks.AITask;
import ui.tasks.AITaskManager;
import util.HttpRequestFormatter;
import util.ThreadManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LLMSecurityAssistantEditorPanel extends JPanel {
    private final MontoyaApi api;
    private final LLMClientFactory clientFactory;
    private final ThreadManager threadManager;
    private final SettingsManager settingsManager;
    private final AITaskManager taskManager;

    private final JTextArea promptArea = new JTextArea(4, 60);
    private final JTextArea notesArea = new JTextArea(4, 60);
    private final JButton sendButton = new JButton("➤");
    private final JLabel statusLabel = new JLabel("Ready");

    private final JToggleButton attachRequest = new JToggleButton("Request", true);
    private final JToggleButton attachResponse = new JToggleButton("Response", false);
    private final JToggleButton attachNotes = new JToggleButton("Notes", false);

    private volatile HttpRequestResponse current;

    public LLMSecurityAssistantEditorPanel(
            MontoyaApi api,
            LLMClientFactory clientFactory,
            ThreadManager threadManager,
            SettingsManager settingsManager,
            AITaskManager taskManager
    ) {
        this.api = api;
        this.clientFactory = clientFactory;
        this.threadManager = threadManager;
        this.settingsManager = settingsManager;
        this.taskManager = taskManager;

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        add(buildTop(), BorderLayout.NORTH);
        add(buildNotes(), BorderLayout.CENTER);
        add(buildStatus(), BorderLayout.SOUTH);

        this.api.userInterface().applyThemeToComponent(this);
    }

    private Component buildTop() {
        JPanel top = new JPanel(new BorderLayout(8, 8));

        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JScrollPane promptScroll = new JScrollPane(promptArea);
        top.add(promptScroll, BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(6, 6));

        sendButton.setToolTipText("Send (creates a task)");
        sendButton.addActionListener(e -> send());
        right.add(sendButton, BorderLayout.NORTH);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chips.add(attachRequest);
        chips.add(attachResponse);
        chips.add(attachNotes);
        right.add(chips, BorderLayout.CENTER);

        top.add(right, BorderLayout.EAST);
        return top;
    }

    private Component buildNotes() {
        JPanel notesPanel = new JPanel(new BorderLayout(4, 4));
        JLabel l = new JLabel("Notes (optional)");
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        notesPanel.add(l, BorderLayout.NORTH);

        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);
        return notesPanel;
    }

    private Component buildStatus() {
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        return statusLabel;
    }

    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.current = requestResponse;
        boolean hasResp = requestResponse != null && requestResponse.response() != null;
        attachResponse.setEnabled(hasResp);
        if (!hasResp) {
            attachResponse.setSelected(false);
        }
    }

    private void send() {
        String userPrompt = promptArea.getText().trim();
        if (userPrompt.isEmpty()) {
            return;
        }

        if (!clientFactory.hasValidApiKey()) {
            JOptionPane.showMessageDialog(this,
                    "Please configure an API key in the extension settings first.",
                    "API Key Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        HttpRequestResponse rr = current;
        String reqText = null;
        String respText = null;
        String notesText = null;

        if (attachRequest.isSelected() && rr != null) {
            reqText = HttpRequestFormatter.formatRequest(rr.request());
        }
        if (attachResponse.isSelected() && rr != null && rr.response() != null) {
            respText = HttpRequestFormatter.formatResponse(rr.response());
        }
        if (attachNotes.isSelected()) {
            String n = notesArea.getText();
            if (n != null && !n.trim().isEmpty()) {
                notesText = n.trim();
            }
        }

        String title = rr != null ? "AI Task: " + HttpRequestFormatter.getRequestSummary(rr) : "AI Task";
        AITask task = new AITask(title, userPrompt, reqText, respText, notesText);
        task.markRunning();
        taskManager.addTask(task);

        // Update UI state
        sendButton.setEnabled(false);
        statusLabel.setText("Sending to " + settingsManager.getActiveProvider().getDisplayName() + "...");

        String fullPrompt = buildFullPrompt(userPrompt, reqText, respText, notesText);

        threadManager.submitAsync(() -> {
            LLMClient client = clientFactory.createClient();
            LLMResponse response = client.complete(fullPrompt, PromptTemplates.CHAT_SYSTEM);

            SwingUtilities.invokeLater(() -> {
                sendButton.setEnabled(true);
                statusLabel.setText("Ready");
                promptArea.setText("");

                if (response.isSuccess()) {
                    task.markCompleted(response.getContent(), response.getTokensUsed());
                } else {
                    task.markFailed(response.getErrorMessage());
                }
                taskManager.notifyUpdated(task);
            });
        }, error -> SwingUtilities.invokeLater(() -> {
            sendButton.setEnabled(true);
            statusLabel.setText("Error occurred");
            task.markFailed(error.getMessage());
            taskManager.notifyUpdated(task);
        }));
    }

    private static String buildFullPrompt(String userPrompt, String reqText, String respText, String notesText) {
        StringBuilder sb = new StringBuilder();
        sb.append(userPrompt.trim());

        if ((reqText != null && !reqText.isBlank())
                || (respText != null && !respText.isBlank())
                || (notesText != null && !notesText.isBlank())) {
            sb.append("\n\n");
        }

        if (reqText != null && !reqText.isBlank()) {
            sb.append("=== Request ===\n").append(reqText).append("\n\n");
        }
        if (respText != null && !respText.isBlank()) {
            sb.append("=== Response ===\n").append(respText).append("\n\n");
        }
        if (notesText != null && !notesText.isBlank()) {
            sb.append("=== Notes ===\n").append(notesText).append("\n");
        }

        return sb.toString().trim();
    }
}


