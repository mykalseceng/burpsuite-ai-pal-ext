package ui.chat;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import config.SettingsManager;
import llm.LLMClientFactory;
import util.ThreadManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChatTab extends JPanel {
    private final MontoyaApi api;
    private final LLMClientFactory clientFactory;
    private final ThreadManager threadManager;
    private final SettingsManager settingsManager;

    private final JTabbedPane sessions = new JTabbedPane();
    private int sessionCounter = 1;

    public ChatTab(MontoyaApi api, LLMClientFactory clientFactory,
                   ThreadManager threadManager, SettingsManager settingsManager) {
        this.api = api;
        this.clientFactory = clientFactory;
        this.threadManager = threadManager;
        this.settingsManager = settingsManager;

        setLayout(new BorderLayout());

        // Default session
        addSession("1");
        add(sessions, BorderLayout.CENTER);

        // Top toolbar / instructions
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JLabel infoLabel = new JLabel("In Proxy, right-click a request → Extensions → AI Pal → Chat (opens in AI Pal → Chat)");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        toolbar.add(infoLabel);

        add(toolbar, BorderLayout.NORTH);
    }

    /**
     * Called by the context menu action to populate the request/response viewers
     * and attach the message to the next chat prompt.
     */
    public void showInChat(HttpRequestResponse requestResponse) {
        if (requestResponse == null) return;

        SwingUtilities.invokeLater(() -> {
            // If current session has no messages and no request set yet, reuse it; otherwise open a new session.
            ChatSessionPanel current = getSelectedSession();
            boolean canReuse = current != null && current.isPristine();

            ChatSessionPanel target = canReuse ? current : addSession(nextSessionTitle());
            target.setRequestResponse(requestResponse);
        });
    }

    private ChatSessionPanel addSession(String title) {
        ChatSessionPanel session = new ChatSessionPanel(api, clientFactory, threadManager, settingsManager);
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
        // Numbered tabs only; user can rename as needed later.
        return String.valueOf(sessionCounter);
    }

    private void closeTab(Component tabComponent) {
        int idx = sessions.indexOfComponent(tabComponent);
        if (idx < 0) return;
        sessions.removeTabAt(idx);

        // Always keep at least one session available.
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

            JButton close = new JButton("×");
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