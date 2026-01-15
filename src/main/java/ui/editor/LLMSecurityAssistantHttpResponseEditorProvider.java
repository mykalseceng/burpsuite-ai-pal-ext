package ui.editor;

import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider;

import config.SettingsManager;
import llm.LLMClientFactory;
import ui.tasks.AITaskManager;
import util.ThreadManager;

public class LLMSecurityAssistantHttpResponseEditorProvider implements HttpResponseEditorProvider {
    private final LLMClientFactory clientFactory;
    private final ThreadManager threadManager;
    private final SettingsManager settingsManager;
    private final AITaskManager taskManager;

    public LLMSecurityAssistantHttpResponseEditorProvider(LLMClientFactory clientFactory,
                                                           ThreadManager threadManager,
                                                           SettingsManager settingsManager,
                                                           AITaskManager taskManager) {
        this.clientFactory = clientFactory;
        this.threadManager = threadManager;
        this.settingsManager = settingsManager;
        this.taskManager = taskManager;
    }

    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext creationContext) {
        LLMSecurityAssistantEditorPanel panel = new LLMSecurityAssistantEditorPanel(
                clientFactory, threadManager, settingsManager, taskManager
        );
        return new LLMSecurityAssistantHttpResponseEditor(creationContext.toolSource(), panel);
    }
}
