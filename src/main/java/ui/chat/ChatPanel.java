package ui.chat;

import config.SettingsManager;
import llm.LLMClient;
import llm.LLMClientFactory;
import llm.LLMResponse;
import llm.prompts.PromptTemplates;
import util.ThreadManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ChatPanel extends JPanel implements ConversationHistory.ConversationListener {
    private final LLMClientFactory clientFactory;
    private final ThreadManager threadManager;
    private final SettingsManager settingsManager;
    private final ConversationHistory history;

    private final JTextPane chatDisplay;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private final JButton stopButton;
    private final JLabel statusLabel;

    private String attachedRequest = null;
    private boolean isProcessing = false;
    private volatile boolean stopRequested = false;

    // Style constants
    private static final Color USER_COLOR = new Color(0, 102, 204);
    private static final Color ASSISTANT_COLOR = new Color(0, 128, 0);
    private static final Color SYSTEM_COLOR = new Color(128, 128, 128);

    public ChatPanel(LLMClientFactory clientFactory, ThreadManager threadManager,
                     SettingsManager settingsManager, ConversationHistory history) {
        this.clientFactory = clientFactory;
        this.threadManager = threadManager;
        this.settingsManager = settingsManager;
        this.history = history;

        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Chat display area
        chatDisplay = new JTextPane();
        chatDisplay.setEditable(false);
        chatDisplay.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane chatScroll = new JScrollPane(chatDisplay);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(chatScroll, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));

        // Input area
        inputArea = new JTextArea(3, 40);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isShiftDown()) {
                    // Shift+Enter for newline
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    sendMessage();
                }
            }
        });
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // Button panel - use GridLayout for vertical stacking
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 5, 5));

        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        buttonPanel.add(sendButton);

        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopStreaming());
        buttonPanel.add(stopButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearChat());
        buttonPanel.add(clearButton);

        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // Status label
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        inputPanel.add(statusLabel, BorderLayout.SOUTH);

        add(inputPanel, BorderLayout.SOUTH);

        // Add system message
        history.addSystemMessage(PromptTemplates.CHAT_SYSTEM);

        // Register as listener
        history.addListener(this);
    }

    public void setAttachedRequest(String requestContent) {
        this.attachedRequest = requestContent;
        if (requestContent != null) {
            statusLabel.setText("Request attached. Type your message.");
        } else {
            statusLabel.setText("Ready");
        }
    }

    private void sendMessage() {
        if (isProcessing) return;

        String userMessage = inputArea.getText().trim();
        if (userMessage.isEmpty()) return;

        if (!clientFactory.hasValidConfig()) {
            JOptionPane.showMessageDialog(this,
                    "Please configure the LLM provider in the extension settings first.",
                    "Configuration Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        isProcessing = true;
        stopRequested = false;
        sendButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Sending to " + settingsManager.getActiveProvider().getDisplayName() + "...");

        // Add user message to history
        if (attachedRequest != null) {
            history.addUserMessage(userMessage, attachedRequest);
            attachedRequest = null;
        } else {
            history.addUserMessage(userMessage);
        }

        inputArea.setText("");

        // Send to LLM
        threadManager.submitAsync(() -> {
            LLMClient client = clientFactory.createClient();

            if (client.supportsStreaming()) {
                // Use streaming for real-time response
                final StringBuilder responseBuilder = new StringBuilder();

                // Add assistant label before streaming starts
                SwingUtilities.invokeLater(() -> {
                    String label = settingsManager.getActiveProvider().getDisplayName();
                    StyledDocument doc = chatDisplay.getStyledDocument();
                    try {
                        SimpleAttributeSet labelStyle = new SimpleAttributeSet();
                        StyleConstants.setBold(labelStyle, true);
                        StyleConstants.setForeground(labelStyle, ASSISTANT_COLOR);
                        doc.insertString(doc.getLength(), label + ": ", labelStyle);
                    } catch (BadLocationException e) {
                        // Ignore
                    }
                    statusLabel.setText("Streaming response...");
                });

                client.chatStreaming(history.getMessages(), "", new LLMClient.StreamCallback() {
                    @Override
                    public boolean isCancelled() {
                        return stopRequested;
                    }

                    @Override
                    public void onChunk(String chunk) {
                        if (stopRequested) {
                            return; // Skip processing if stop was requested
                        }
                        responseBuilder.append(chunk);
                        SwingUtilities.invokeLater(() -> {
                            StyledDocument doc = chatDisplay.getStyledDocument();
                            try {
                                SimpleAttributeSet msgStyle = new SimpleAttributeSet();
                                StyleConstants.setForeground(msgStyle, Color.BLACK);
                                doc.insertString(doc.getLength(), chunk, msgStyle);
                                chatDisplay.setCaretPosition(doc.getLength());
                            } catch (BadLocationException e) {
                                // Ignore
                            }
                        });
                    }

                    @Override
                    public void onComplete(int totalTokens) {
                        SwingUtilities.invokeLater(() -> {
                            // Add newlines at the end
                            StyledDocument doc = chatDisplay.getStyledDocument();
                            try {
                                doc.insertString(doc.getLength(), "\n\n", null);
                            } catch (BadLocationException e) {
                                // Ignore
                            }

                            // Add to history (without triggering onMessageAdded display)
                            String fullResponse = responseBuilder.toString();
                            if (!fullResponse.isEmpty()) {
                                history.addAssistantMessageSilent(fullResponse);
                            }

                            isProcessing = false;
                            sendButton.setEnabled(true);
                            stopButton.setEnabled(false);
                            if (stopRequested) {
                                statusLabel.setText("Stopped");
                            } else {
                                statusLabel.setText("Ready (Tokens used: " + totalTokens + ")");
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        SwingUtilities.invokeLater(() -> {
                            appendMessage("Error", error, Color.RED);
                            isProcessing = false;
                            sendButton.setEnabled(true);
                            stopButton.setEnabled(false);
                            statusLabel.setText("Error occurred");
                        });
                    }
                });
            } else {
                // Fallback to non-streaming
                LLMResponse response = client.chat(history.getMessages(), "");

                SwingUtilities.invokeLater(() -> {
                    isProcessing = false;
                    sendButton.setEnabled(true);
                    stopButton.setEnabled(false);

                    if (response.isSuccess()) {
                        history.addAssistantMessage(response.getContent());
                        statusLabel.setText("Ready (Tokens used: " + response.getTokensUsed() + ")");
                    } else {
                        appendMessage("Error", response.getErrorMessage(), Color.RED);
                        statusLabel.setText("Error occurred");
                    }
                });
            }
        }, error -> SwingUtilities.invokeLater(() -> {
            isProcessing = false;
            sendButton.setEnabled(true);
            stopButton.setEnabled(false);
            appendMessage("Error", error.getMessage(), Color.RED);
            statusLabel.setText("Error occurred");
        }));
    }

    private void stopStreaming() {
        stopRequested = true;
        stopButton.setEnabled(false);
        statusLabel.setText("Stopping...");
    }

    private void clearChat() {
        history.clear();
        chatDisplay.setText("");
        history.addSystemMessage(PromptTemplates.CHAT_SYSTEM);
        statusLabel.setText("Chat cleared");
    }

    @Override
    public void onMessageAdded(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            Color color = switch (message.getRole()) {
                case USER -> USER_COLOR;
                case ASSISTANT -> ASSISTANT_COLOR;
                case SYSTEM -> SYSTEM_COLOR;
            };

            String label = switch (message.getRole()) {
                case USER -> "You";
                case ASSISTANT -> settingsManager.getActiveProvider().getDisplayName();
                case SYSTEM -> "System";
            };

            if (message.getRole() != ChatMessage.Role.SYSTEM) {
                appendMessage(label, message.getContent(), color);
                if (message.hasAttachedRequest()) {
                    appendMessage("", "[Request attached]", SYSTEM_COLOR);
                }
            }
        });
    }

    @Override
    public void onHistoryCleared() {
        SwingUtilities.invokeLater(() -> chatDisplay.setText(""));
    }

    private void appendMessage(String label, String message, Color color) {
        StyledDocument doc = chatDisplay.getStyledDocument();

        try {
            // Label style
            SimpleAttributeSet labelStyle = new SimpleAttributeSet();
            StyleConstants.setBold(labelStyle, true);
            StyleConstants.setForeground(labelStyle, color);

            // Message style
            SimpleAttributeSet msgStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(msgStyle, Color.BLACK);

            if (!label.isEmpty()) {
                doc.insertString(doc.getLength(), label + ": ", labelStyle);
            }
            doc.insertString(doc.getLength(), message + "\n\n", msgStyle);

            // Scroll to bottom
            chatDisplay.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            // Ignore
        }
    }
}