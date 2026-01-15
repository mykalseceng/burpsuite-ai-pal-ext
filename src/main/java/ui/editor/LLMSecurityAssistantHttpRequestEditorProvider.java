package ui.editor;

import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.HttpRequestEditorProvider;

import config.SettingsManager;
import llm.LLMClientFactory;
import ui.tasks.AITaskManager;
import util.ThreadManager;

public class LLMSecurityAssistantHttpRequestEditorProvider implements HttpRequestEditorProvider {
    private final LLMClientFactory clientFactory;
    private final ThreadManager threadManager;
    private final SettingsManager settingsManager;
    private final AITaskManager taskManager;

    public LLMSecurityAssistantHttpRequestEditorProvider(LLMClientFactory clientFactory,
                                                          ThreadManager threadManager,
                                                          SettingsManager settingsManager,
                                                          AITaskManager taskManager) {
        this.clientFactory = clientFactory;
        this.threadManager = threadManager;
        this.settingsManager = settingsManager;
        this.taskManager = taskManager;
    }

    @Override
    public ExtensionProvidedHttpRequestEditor provideHttpRequestEditor(EditorCreationContext creationContext) {
        LLMSecurityAssistantEditorPanel panel = new LLMSecurityAssistantEditorPanel(
                clientFactory, threadManager, settingsManager, taskManager
        );
        return new LLMSecurityAssistantHttpRequestEditor(creationContext.toolSource(), panel);
    }
}
