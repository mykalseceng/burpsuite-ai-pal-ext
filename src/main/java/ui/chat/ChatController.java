package ui.chat;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import ui.AIPalSuiteTab;

public class ChatController {
    private final MontoyaApi api;
    private final ChatTab chatTab;
    private AIPalSuiteTab suiteTab;

    public ChatController(MontoyaApi api, ChatTab chatTab) {
        this.api = api;
        this.chatTab = chatTab;
    }

    /**
     * Set the suite tab reference for tab selection functionality.
     * Called after AIPalSuiteTab is created.
     */
    public void setSuiteTab(AIPalSuiteTab suiteTab) {
        this.suiteTab = suiteTab;
    }

    public void sendToChat(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            return;
        }

        chatTab.showInChat(requestResponse);

        // Select the AI Pal suite tab and bring it to focus with orange underline
        if (suiteTab != null) {
            suiteTab.selectTab(AIPalSuiteTab.CHAT_TAB);
        }
    }
}


