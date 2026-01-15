package ui.tasks;

import static base.Api.api;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class AITasksTab extends JPanel implements AITaskManager.Listener {
    private final AITaskManager taskManager;

    private final DefaultListModel<AITask> listModel = new DefaultListModel<>();
    private final JList<AITask> taskList = new JList<>(listModel);

    private final JTextArea summaryArea = new JTextArea();
    private final JTextArea promptArea = new JTextArea();
    private final JTextArea resultArea = new JTextArea();
    private final JTextArea reqResArea = new JTextArea();

    public AITasksTab(AITaskManager taskManager) {
        this.taskManager = taskManager;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.25);

        split.setLeftComponent(buildLeft());
        split.setRightComponent(buildRight());
        add(split, BorderLayout.CENTER);

        for (AITask t : this.taskManager.tasks()) {
            listModel.addElement(t);
        }

        taskList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                renderSelected();
            }
        });

        this.taskManager.addListener(this);
        api.userInterface().applyThemeToComponent(this);
    }

    @Override
    public void onTaskAdded(AITask task) {
        SwingUtilities.invokeLater(() -> {
            listModel.add(0, task);
            if (listModel.size() == 1) {
                taskList.setSelectedIndex(0);
            }
        });
    }

    @Override
    public void onTaskUpdated(AITask task) {
        SwingUtilities.invokeLater(() -> {
            taskList.repaint();
            if (task == taskList.getSelectedValue()) {
                renderSelected();
            }
        });
    }

    private Component buildLeft() {
        JPanel left = new JPanel(new BorderLayout(8, 8));

        JLabel title = new JLabel("Tasks");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        left.add(title, BorderLayout.NORTH);

        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setCellRenderer(new TaskCellRenderer());
        JScrollPane scroll = new JScrollPane(taskList);
        left.add(scroll, BorderLayout.CENTER);

        return left;
    }

    private Component buildRight() {
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout(8, 8));

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Task details");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        header.add(title, BorderLayout.WEST);
        right.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(section("Task summary", summaryArea, 5));
        content.add(Box.createVerticalStrut(8));
        content.add(section("Prompt", promptArea, 4));
        content.add(Box.createVerticalStrut(8));
        content.add(section("Result", resultArea, 10));
        content.add(Box.createVerticalStrut(8));
        content.add(section("Request / Response / Notes", reqResArea, 10));

        JScrollPane scroll = new JScrollPane(content);
        right.add(scroll, BorderLayout.CENTER);

        configureTextArea(summaryArea);
        configureTextArea(promptArea);
        configureTextArea(resultArea);
        configureTextArea(reqResArea);

        return right;
    }

    private void renderSelected() {
        AITask t = taskList.getSelectedValue();
        if (t == null) {
            summaryArea.setText("");
            promptArea.setText("");
            resultArea.setText("");
            reqResArea.setText("");
            return;
        }

        StringBuilder summary = new StringBuilder();
        summary.append(t.title()).append("\n");
        summary.append("Status: ").append(t.status()).append("\n");
        if (t.tokensUsed() != null) {
            summary.append("Tokens used: ").append(t.tokensUsed()).append("\n");
        }
        if (t.error() != null) {
            summary.append("\nError:\n").append(t.error());
        }
        summaryArea.setText(summary.toString());
        summaryArea.setCaretPosition(0);

        promptArea.setText(t.prompt());
        promptArea.setCaretPosition(0);

        if (t.result() != null) {
            resultArea.setText(t.result());
        } else if (t.status() == AITaskStatus.RUNNING) {
            resultArea.setText("Running...");
        } else {
            resultArea.setText("");
        }
        resultArea.setCaretPosition(0);

        StringBuilder rr = new StringBuilder();
        if (t.requestText() != null && !t.requestText().isBlank()) {
            rr.append("=== Request ===\n").append(t.requestText()).append("\n\n");
        }
        if (t.responseText() != null && !t.responseText().isBlank()) {
            rr.append("=== Response ===\n").append(t.responseText()).append("\n\n");
        }
        if (t.notesText() != null && !t.notesText().isBlank()) {
            rr.append("=== Notes ===\n").append(t.notesText()).append("\n");
        }
        reqResArea.setText(rr.toString());
        reqResArea.setCaretPosition(0);
    }

    private static void configureTextArea(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    }

    private static JComponent section(String label, JTextArea area, int rows) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        p.add(l, BorderLayout.NORTH);
        area.setRows(rows);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }

    private static class TaskCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof AITask t) {
                String status = switch (t.status()) {
                    case PENDING -> "\u23f3";
                    case RUNNING -> "\u25b6";
                    case COMPLETED -> "\u2713";
                    case FAILED -> "!";
                };
                setText(status + " " + t.title());
            }
            return this;
        }
    }
}
