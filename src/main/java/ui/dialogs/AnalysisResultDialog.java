package ui.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class AnalysisResultDialog extends JDialog {
    private final JTextArea resultArea;

    // Consistent styling like Atlas-AI
    private static final Font RESULT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    private static final Color BACKGROUND_COLOR = new Color(250, 250, 250);

    public AnalysisResultDialog(Frame parent, String title, String result) {
        super(parent, title, false); // Non-modal so user can interact with Burp

        setSize(900, 700);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Bold header at top
        JLabel headerLabel = new JLabel("AI PAL SECURITY ANALYSIS");
        headerLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        headerLabel.setBorder(new EmptyBorder(10, 15, 5, 15));
        add(headerLabel, BorderLayout.NORTH);

        // Result text area
        resultArea = new JTextArea(result);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(RESULT_FONT);
        resultArea.setBackground(BACKGROUND_COLOR);
        resultArea.setMargin(new Insets(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 10, 5, 10),
                BorderFactory.createLineBorder(new Color(200, 200, 200))
        ));
        add(scrollPane, BorderLayout.CENTER);

        // Button panel with better styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(resultArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(this, "Copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(copyButton);

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void appendResult(String text) {
        SwingUtilities.invokeLater(() -> {
            resultArea.append(text);
            resultArea.setCaretPosition(resultArea.getDocument().getLength());
        });
    }

    public void setResult(String text) {
        SwingUtilities.invokeLater(() -> {
            resultArea.setText(text);
            resultArea.setCaretPosition(0);
        });
    }

    public static void showResult(Frame parent, String title, String result) {
        AnalysisResultDialog dialog = new AnalysisResultDialog(parent, title, result);
        dialog.setVisible(true);
    }

    public static AnalysisResultDialog showLoading(Frame parent, String title) {
        AnalysisResultDialog dialog = new AnalysisResultDialog(parent, title, "Analyzing... Please wait.");
        dialog.setVisible(true);
        return dialog;
    }
}