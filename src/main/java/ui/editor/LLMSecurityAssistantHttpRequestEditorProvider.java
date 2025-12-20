package ui.editor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;
import config.SettingsManager;
import llm.LLMClientFactory;
import ui.tasks.AITaskManager;
import util.ThreadManager;

public class LLMSecurityAssistantHttpRequestEditorProvider implements HttpRequestEditorProvider {
    private final MontoyaApi api;
    private final LLMClientFactory clientFactory;
    private final ThreadManager threadManager;
    private final SettingsManager settingsManager;
    private final AITaskManager taskManager;

    public LLMSecurityAssistantHttpRequestEditorProvider(
            MontoyaApi api,
            LLMClientFactory clientFactory,
            ThreadManager threadManager,
            SettingsManager settingsManager,
            AITaskManager taskManager
    ) {
        this.api = api;
        this.clientFactory = clientFactory;
        this.threadManager = threadManager;
        this.settingsManager = settingsManager;
        this.taskManager = taskManager;
    }

    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext creationContext) {
        LLMSecurityAssistantEditorPanel panel = new LLMSecurityAssistantEditorPanel(
                api, clientFactory, threadManager, settingsManager, taskManager
        );
        return new LLMSecurityAssistantHttpRequestEditor(creationContext.toolSource(), panel);
    }
}


