import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import config.SettingsManager;
import llm.LLMClientFactory;
import ui.chat.ChatController;
import ui.editor.LLMSecurityAssistantHttpRequestEditorProvider;
import ui.editor.LLMSecurityAssistantHttpResponseEditorProvider;
import ui.chat.ChatTab;
import ui.contextmenu.LLMContextMenuProvider;
import ui.settings.LLMSettingsPanel;
import ui.tasks.AITaskManager;
import ui.tasks.AITasksTab;
import ui.AIPalSuiteTab;
import util.ThreadManager;

public class Extension implements BurpExtension {
    private ThreadManager threadManager;
    private SettingsManager settingsManager;
    private LLMClientFactory clientFactory;
    private AITaskManager taskManager;

    @Override
    public void initialize(MontoyaApi api) {
        // Set extension name
        api.extension().setName("AI Pal");

        // Initialize core services
        this.threadManager = new ThreadManager(api.logging());
        this.settingsManager = new SettingsManager(api.persistence().preferences());
        this.clientFactory = new LLMClientFactory(api.http(), api.logging(), settingsManager);
        this.taskManager = new AITaskManager();

        // Build AI Pal single suite tab with internal tabs
        LLMSettingsPanel settingsPanel = new LLMSettingsPanel(settingsManager, clientFactory);
        AITasksTab aiTasksTab = new AITasksTab(api, taskManager);
        ChatTab chatTab = new ChatTab(api, clientFactory, threadManager, settingsManager);

        AIPalSuiteTab aiPalSuiteTab = new AIPalSuiteTab(api, chatTab, aiTasksTab, settingsPanel);
        api.userInterface().registerSuiteTab("AI Pal", aiPalSuiteTab);

        // Register context menu provider (includes "Chat" action)
        ChatController chatController = new ChatController(api, chatTab);
        chatController.setSuiteTab(aiPalSuiteTab);  // Enable tab selection with orange underline
        api.userInterface().registerContextMenuItemsProvider(
                new LLMContextMenuProvider(api, clientFactory, threadManager, settingsManager, chatController)
        );

        // Register editor tabs for Repeater request/response editors
        api.userInterface().registerHttpRequestEditorProvider(
                new LLMSecurityAssistantHttpRequestEditorProvider(api, clientFactory, threadManager, settingsManager, taskManager)
        );
        api.userInterface().registerHttpResponseEditorProvider(
                new LLMSecurityAssistantHttpResponseEditorProvider(api, clientFactory, threadManager, settingsManager, taskManager)
        );

        // Register cleanup handler
        api.extension().registerUnloadingHandler(() -> {
            threadManager.shutdown();
            api.logging().logToOutput("AI Pal unloaded");
        });

        // Log success
        api.logging().logToOutput("AI Pal loaded successfully");
        api.logging().logToOutput("Configure API keys in the AI Pal Settings tab to get started");
    }
}