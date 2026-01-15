package ui;

import static base.Api.api;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class AIPalSuiteTab extends JPanel {
    public static final int CHAT_TAB = 0;
    public static final int TASKS_TAB = 1;
    public static final int SETTINGS_TAB = 2;

    private final JTabbedPane internalTabs;

    public AIPalSuiteTab(Component chat, Component tasks, Component settings) {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(6, 6, 6, 6));

        internalTabs = new JTabbedPane();
        internalTabs.addTab("Chat", chat);
        internalTabs.addTab("Tasks", tasks);
        internalTabs.addTab("Settings", settings);

        add(internalTabs, BorderLayout.CENTER);

        api.userInterface().applyThemeToComponent(this);
    }

    public void selectTab(int tabIndex) {
        SwingUtilities.invokeLater(() -> {
            if (tabIndex >= 0 && tabIndex < internalTabs.getTabCount()) {
                internalTabs.setSelectedIndex(tabIndex);
            }
            selectSuiteTab();
        });
    }

    public void highlightChat() {
        selectTab(CHAT_TAB);
    }

    private void selectSuiteTab() {
        try {
            Container parent = this.getParent();
            while (parent != null) {
                if (parent instanceof JTabbedPane suiteTabPane) {
                    for (int i = 0; i < suiteTabPane.getTabCount(); i++) {
                        if (suiteTabPane.getComponentAt(i) == this) {
                            suiteTabPane.setSelectedIndex(i);
                            break;
                        }
                    }
                    break;
                }
                parent = parent.getParent();
            }

            Frame frame = api.userInterface().swingUtils().suiteFrame();
            if (frame != null) {
                frame.toFront();
                frame.requestFocus();
            }
        } catch (Exception e) {
            // Best effort
        }
    }
}
