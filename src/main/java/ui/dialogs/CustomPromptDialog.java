package ui.dialogs;

import javax.swing.*;
import java.awt.*;

import ui.UIStyle;

public class CustomPromptDialog extends JDialog {
    private final JTextArea promptArea;
    private final JTextArea previewArea;
    private String result = null;

    public CustomPromptDialog(Frame parent, String requestPreview) {
        super(parent, "Custom Prompt", true);

        setSize(700, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);

        // Prompt input panel
        JPanel promptPanel = new JPanel(new BorderLayout(5, 5));
        promptPanel.setBorder(UIStyle.createSectionBorder("Enter your prompt"));
        promptArea = new JTextArea();
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        promptArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        UIStyle.applyTextInputPadding(promptArea);
        JScrollPane promptScroll = new JScrollPane(promptArea);
        promptScroll.setBorder(UIStyle.createFieldBorder());
        promptPanel.add(promptScroll, BorderLayout.CENTER);
        splitPane.setTopComponent(promptPanel);

        // Request preview panel
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));
        previewPanel.setBorder(UIStyle.createSectionBorder("Request/Response context"));
        previewArea = new JTextArea(requestPreview);
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        previewArea.setBackground(new Color(245, 245, 245));
        UIStyle.applyTextInputPadding(previewArea);
        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setBorder(UIStyle.createFieldBorder());
        previewPanel.add(previewScroll, BorderLayout.CENTER);
        splitPane.setBottomComponent(previewPanel);

        add(splitPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton sendButton = new JButton("Send to LLM");
        sendButton.addActionListener(e -> {
            String prompt = promptArea.getText().trim();
            if (prompt.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please enter a prompt.",
                        "Empty Prompt",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            result = prompt;
            dispose();
        });
        buttonPanel.add(sendButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            result = null;
            dispose();
        });
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Set default button
        getRootPane().setDefaultButton(sendButton);
    }

    public String getPrompt() {
        return result;
    }

    public static String showDialog(Frame parent, String requestPreview) {
        CustomPromptDialog dialog = new CustomPromptDialog(parent, requestPreview);
        dialog.setVisible(true);
        return dialog.getPrompt();
    }
}
