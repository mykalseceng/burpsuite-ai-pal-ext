package ui.chat;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;

import java.awt.*;

public class ChatController {
    private final MontoyaApi api;
    private final ChatTab chatTab;

    public ChatController(MontoyaApi api, ChatTab chatTab) {
        this.api = api;
        this.chatTab = chatTab;
    }

    public void sendToChat(HttpRequestResponse requestResponse) {
        if (requestResponse == null) {
            return;
        }

        chatTab.showInChat(requestResponse);

        // We can't programmatically select a suite tab via Montoya, but we can at least bring Burp to front.
        try {
            Frame f = api.userInterface().swingUtils().suiteFrame();
            f.toFront();
            f.requestFocus();
        } catch (Exception ignored) {
            // best-effort only
        }
    }
}


