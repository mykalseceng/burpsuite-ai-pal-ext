import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

import base.Api;
import config.SettingsManager;
import llm.LLMClientFactory;
import ui.AIPalSuiteTab;
import ui.chat.ChatController;
import ui.chat.ChatTab;
import ui.contextmenu.LLMContextMenuProvider;
import ui.editor.LLMSecurityAssistantHttpRequestEditorProvider;
import ui.editor.LLMSecurityAssistantHttpResponseEditorProvider;
import ui.settings.LLMSettingsPanel;
import ui.tasks.AITaskManager;
import ui.tasks.AITasksTab;
import util.ThreadManager;

public class Extension implements BurpExtension {
    private ThreadManager threadManager;
    private SettingsManager settingsManager;
    private LLMClientFactory clientFactory;
    private AITaskManager taskManager;

    @Override
    public void initialize(MontoyaApi montoyaApi) {
        Api.api = montoyaApi;
        Api.api.extension().setName(Api.extensionName);

        threadManager = new ThreadManager();
        settingsManager = new SettingsManager(Api.api.persistence().preferences());
        clientFactory = new LLMClientFactory(settingsManager);
        taskManager = new AITaskManager();

        LLMSettingsPanel settingsPanel = new LLMSettingsPanel(settingsManager, clientFactory);
        AITasksTab aiTasksTab = new AITasksTab(taskManager);
        ChatTab chatTab = new ChatTab(clientFactory, threadManager, settingsManager);

        AIPalSuiteTab aiPalSuiteTab = new AIPalSuiteTab(chatTab, aiTasksTab, settingsPanel);
        Api.api.userInterface().registerSuiteTab(Api.extensionName, aiPalSuiteTab);

        ChatController chatController = new ChatController(chatTab);
        chatController.setSuiteTab(aiPalSuiteTab);
        Api.api.userInterface().registerContextMenuItemsProvider(
                new LLMContextMenuProvider(clientFactory, threadManager, settingsManager, chatController)
        );

        Api.api.userInterface().registerHttpRequestEditorProvider(
                new LLMSecurityAssistantHttpRequestEditorProvider(clientFactory, threadManager, settingsManager, taskManager)
        );
        Api.api.userInterface().registerHttpResponseEditorProvider(
                new LLMSecurityAssistantHttpResponseEditorProvider(clientFactory, threadManager, settingsManager, taskManager)
        );

        Api.api.extension().registerUnloadingHandler(() -> {
            threadManager.shutdown();
            Api.api.logging().logToOutput(Api.extensionName + " unloaded");
        });

        Api.api.logging().logToOutput(Api.extensionName + " " + Api.version + " loaded successfully");
        Api.api.logging().logToOutput("Configure API keys in the AI Pal Settings tab to get started");
    }
}
