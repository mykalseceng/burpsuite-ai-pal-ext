package ui.editor;

import burp.api.montoya.core.ToolSource;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;

import java.awt.*;

public class LLMSecurityAssistantHttpRequestEditor implements ExtensionProvidedHttpRequestEditor {
    private final ToolSource toolSource;
    private final LLMSecurityAssistantEditorPanel panel;
    private volatile HttpRequestResponse current;

    public LLMSecurityAssistantHttpRequestEditor(ToolSource toolSource, LLMSecurityAssistantEditorPanel panel) {
        this.toolSource = toolSource;
        this.panel = panel;
    }

    @Override
    public String caption() {
        return "AI Pal";
    }

    @Override
    public Component uiComponent() {
        return panel;
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.current = requestResponse;
        panel.setRequestResponse(requestResponse);
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        // Match the "source of truth" behavior: only show inside Repeater.
        return toolSource != null && toolSource.isFromTool(ToolType.REPEATER);
    }

    @Override
    public HttpRequest getRequest() {
        HttpRequestResponse rr = current;
        return rr != null ? rr.request() : null;
    }

    @Override
    public Selection selectedData() {
        return null;
    }

    @Override
    public boolean isModified() {
        return false;
    }
}


