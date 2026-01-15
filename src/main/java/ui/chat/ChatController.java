package ui.chat;

import burp.api.montoya.http.message.HttpRequestResponse;

import ui.AIPalSuiteTab;

public class ChatController {
    private final ChatTab chatTab;
    private AIPalSuiteTab suiteTab;

    public ChatController(ChatTab chatTab) {
        this.chatTab = chatTab;
    }

    public void setSuiteTab(AIPalSuiteTab suiteTab) {
        this.suiteTab = suiteTab;
    }

    public void sendToChat(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            return;
        }

        chatTab.showInChat(requestResponse);

        if (suiteTab != null) {
            suiteTab.selectTab(AIPalSuiteTab.CHAT_TAB);
        }
    }
}
