package ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class AnalysisResultDialog extends JDialog {
    private final JTextArea resultArea;

    public AnalysisResultDialog(Frame parent, String title, String result) {
        super(parent, title, false); // Non-modal so user can interact with Burp

        setSize(800, 600);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // Result text area
        resultArea = new JTextArea(result);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            StringSelection selection = new StringSelection(resultArea.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            JOptionPane.showMessageDialog(this, "Copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(copyButton);

        JButton closeButton = new JButton("Close");
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