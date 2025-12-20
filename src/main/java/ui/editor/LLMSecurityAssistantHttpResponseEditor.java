package ui.editor;

import burp.api.montoya.core.ToolSource;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;

import java.awt.*;

public class LLMSecurityAssistantHttpResponseEditor implements ExtensionProvidedHttpResponseEditor {
    private final ToolSource toolSource;
    private final LLMSecurityAssistantEditorPanel panel;
    private volatile HttpRequestResponse current;

    public LLMSecurityAssistantHttpResponseEditor(ToolSource toolSource, LLMSecurityAssistantEditorPanel panel) {
        this.toolSource = toolSource;
        this.panel = panel;
    }

    @Override
    public String caption() {
        return "LLM Security Assistant";
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
        return toolSource != null && toolSource.isFromTool(ToolType.REPEATER);
    }

    @Override
    public HttpResponse getResponse() {
        HttpRequestResponse rr = current;
        return rr != null ? rr.response() : null;
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


