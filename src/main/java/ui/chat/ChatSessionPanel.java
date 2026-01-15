package ui.chat;

import static base.Api.api;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.HttpRequestEditor;
import burp.api.montoya.ui.editor.HttpResponseEditor;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import config.SettingsManager;
import llm.LLMClientFactory;
import util.HttpRequestFormatter;
import util.ThreadManager;

public class ChatSessionPanel extends JPanel {
    private final ConversationHistory history;
    private final ChatPanel chatPanel;
    private final HttpRequestEditor requestEditor;
    private final HttpResponseEditor responseEditor;

    private volatile HttpRequestResponse current;

    public ChatSessionPanel(LLMClientFactory clientFactory, ThreadManager threadManager, SettingsManager settingsManager) {
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

        String formatted = HttpRequestFormatter.format(requestResponse);
        chatPanel.setAttachedRequest(formatted);
    }

    public HttpRequestResponse current() {
        return current;
    }

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

    private Component createMessagePanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);

        split.setLeftComponent(requestEditor.uiComponent());
        split.setRightComponent(responseEditor.uiComponent());
        return split;
    }
}
