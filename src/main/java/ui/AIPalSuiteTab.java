package ui;

import burp.api.montoya.MontoyaApi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Single top-level suite tab for AI Pal, with internal tabs for Chat / Tasks / Settings.
 */
public class AIPalSuiteTab extends JPanel {
    public AIPalSuiteTab(MontoyaApi api, Component chat, Component tasks, Component settings) {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(6, 6, 6, 6));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Chat", chat);
        tabs.addTab("Tasks", tasks);
        tabs.addTab("Settings", settings);

        add(tabs, BorderLayout.CENTER);

        api.userInterface().applyThemeToComponent(this);
    }
}


