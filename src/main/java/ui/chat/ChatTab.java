package ui.chat;

import burp.api.montoya.http.message.HttpRequestResponse;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import config.SettingsManager;
import llm.LLMClientFactory;
import util.ThreadManager;

public class ChatTab extends JPanel {
    private final LLMClientFactory clientFactory;
    private final ThreadManager threadManager;
    private final SettingsManager settingsManager;

    private final JTabbedPane sessions = new JTabbedPane();
    private int sessionCounter = 1;

    public ChatTab(LLMClientFactory clientFactory, ThreadManager threadManager, SettingsManager settingsManager) {
        this.clientFactory = clientFactory;
        this.threadManager = threadManager;
        this.settingsManager = settingsManager;

        setLayout(new BorderLayout());

        addSession("1");
        add(sessions, BorderLayout.CENTER);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JLabel infoLabel = new JLabel("In Proxy, right-click a request -> Extensions -> AI Pal -> Chat (opens in AI Pal -> Chat)");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        toolbar.add(infoLabel);

        add(toolbar, BorderLayout.NORTH);
    }

    public void showInChat(HttpRequestResponse requestResponse) {
        if (requestResponse == null) return;

        SwingUtilities.invokeLater(() -> {
            ChatSessionPanel current = getSelectedSession();
            boolean canReuse = current != null && current.isPristine();

            ChatSessionPanel target = canReuse ? current : addSession(nextSessionTitle());
            target.setRequestResponse(requestResponse);
        });
    }

    private ChatSessionPanel addSession(String title) {
        ChatSessionPanel session = new ChatSessionPanel(clientFactory, threadManager, settingsManager);
        sessions.addTab(title, session);
        int idx = sessions.indexOfComponent(session);
        sessions.setSelectedIndex(idx);
        sessions.setTabComponentAt(idx, new ClosableTabHeader(sessions, title));
        sessionCounter++;
        return session;
    }

    private ChatSessionPanel getSelectedSession() {
        Component c = sessions.getSelectedComponent();
        return (c instanceof ChatSessionPanel sp) ? sp : null;
    }

    private String nextSessionTitle() {
        return String.valueOf(sessionCounter);
    }

    private void closeTab(Component tabComponent) {
        int idx = sessions.indexOfComponent(tabComponent);
        if (idx < 0) return;
        sessions.removeTabAt(idx);

        if (sessions.getTabCount() == 0) {
            sessionCounter = 1;
            addSession("1");
        }
    }

    private final class ClosableTabHeader extends JPanel {
        ClosableTabHeader(JTabbedPane pane, String title) {
            super(new FlowLayout(FlowLayout.LEFT, 6, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 0, 0));

            JLabel label = new JLabel(title);
            add(label);

            JButton close = new JButton("\u00d7");
            close.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            close.setContentAreaFilled(false);
            close.setFocusable(false);
            close.setToolTipText("Close");
            close.addActionListener(e -> {
                int idx = pane.indexOfTabComponent(this);
                if (idx >= 0) {
                    Component tab = pane.getComponentAt(idx);
                    closeTab(tab);
                }
            });
            add(close);
        }
    }
}
