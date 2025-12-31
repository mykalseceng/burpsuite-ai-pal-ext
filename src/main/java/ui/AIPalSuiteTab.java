package ui;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Single top-level suite tab for AI Pal, with internal tabs for Chat / Tasks / Settings.
 */
public class AIPalSuiteTab extends JPanel {
    private final MontoyaApi api;
    private final JTabbedPane internalTabs;

    // Tab indices
    public static final int CHAT_TAB = 0;
    public static final int TASKS_TAB = 1;
    public static final int SETTINGS_TAB = 2;

    public AIPalSuiteTab(MontoyaApi api, Component chat, Component tasks, Component settings) {
        this.api = api;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(6, 6, 6, 6));

        internalTabs = new JTabbedPane();
        internalTabs.addTab("Chat", chat);
        internalTabs.addTab("Tasks", tasks);
        internalTabs.addTab("Settings", settings);

        add(internalTabs, BorderLayout.CENTER);

        api.userInterface().applyThemeToComponent(this);
    }

    /**
     * Select a specific internal tab and bring the AI Pal tab to focus.
     * @param tabIndex The tab index (CHAT_TAB, TASKS_TAB, or SETTINGS_TAB)
     */
    public void selectTab(int tabIndex) {
        SwingUtilities.invokeLater(() -> {
            // Select the internal tab
            if (tabIndex >= 0 && tabIndex < internalTabs.getTabCount()) {
                internalTabs.setSelectedIndex(tabIndex);
            }

            // Try to select the AI Pal suite tab in Burp's main tabbed pane
            selectSuiteTab();
        });
    }

    /**
     * Attempt to select this suite tab in Burp's main tab bar.
     */
    private void selectSuiteTab() {
        try {
            // Walk up the component hierarchy to find the parent JTabbedPane
            Container parent = this.getParent();
            while (parent != null) {
                if (parent instanceof JTabbedPane) {
                    JTabbedPane suiteTabPane = (JTabbedPane) parent;
                    // Find our index
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

            // Also bring Burp to front
            Frame frame = api.userInterface().swingUtils().suiteFrame();
            if (frame != null) {
                frame.toFront();
                frame.requestFocus();
            }
        } catch (Exception e) {
            // Best effort - ignore errors
        }
    }

    /**
     * Highlight the chat tab (convenience method).
     */
    public void highlightChat() {
        selectTab(CHAT_TAB);
    }
}


