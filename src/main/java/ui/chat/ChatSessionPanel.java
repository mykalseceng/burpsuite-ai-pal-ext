package ui.chat;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;
import config.SettingsManager;
import llm.LLMClientFactory;
import util.HttpRequestFormatter;
import util.ThreadManager;

import javax.swing.*;
import java.awt.*;

/**
 * One chat "session" (like a Repeater tab): request/response viewers + isolated chat history.
 */
public class ChatSessionPanel extends JPanel {
    private final ConversationHistory history;
    private final ChatPanel chatPanel;
    private final HttpRequestEditor requestEditor;
    private final HttpResponseEditor responseEditor;

    private volatile HttpRequestResponse current;

    public ChatSessionPanel(
            MontoyaApi api,
            LLMClientFactory clientFactory,
            ThreadManager threadManager,
            SettingsManager settingsManager
    ) {
        this.history = new ConversationHistory();
        this.requestEditor = api.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY);
        this.responseEditor = api.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY);
        this.chatPanel = new ChatPanel(clientFactory, threadManager, settingsManager, history);

        setLayout(new BorderLayout());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setResizeWeight(0.65);

        mainSplit.setLeftComponent(createMessagePanel());
        mainSplit.setRightComponent(chatPanel);

        add(mainSplit, BorderLayout.CENTER);
        api.userInterface().applyThemeToComponent(this);
    }

    private Component createMessagePanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);

        split.setLeftComponent(requestEditor.uiComponent());
        split.setRightComponent(responseEditor.uiComponent());
        return split;
    }

    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.current = requestResponse;
        if (requestResponse == null) {
            return;
        }

        requestEditor.setRequest(requestResponse.request());
        if (requestResponse.response() != null) {
            responseEditor.setResponse(requestResponse.response());
        } else {
            responseEditor.setResponse(HttpResponse.httpResponse());
        }

        // Attach full request/response into the next user prompt in this session.
        String formatted = HttpRequestFormatter.format(requestResponse);
        chatPanel.setAttachedRequest(formatted);
    }

    public HttpRequestResponse current() {
        return current;
    }

    /**
     * A "blank" session should be reusable for the first incoming request.
     * Note: {@link ChatPanel} adds a SYSTEM message on construction, so we ignore system-only history here.
     */
    public boolean isPristine() {
        if (current != null) {
            return false;
        }
        for (ChatMessage m : history.getMessages()) {
            if (m.getRole() != ChatMessage.Role.SYSTEM) {
                return false;
            }
        }
        return true;
    }
}


