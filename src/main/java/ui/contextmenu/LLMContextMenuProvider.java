package ui.contextmenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import config.SettingsManager;
import llm.LLMClient;
import llm.LLMClientFactory;
import llm.LLMResponse;
import llm.prompts.PromptTemplates;
import ui.chat.ChatController;
import ui.dialogs.AnalysisResultDialog;
import ui.dialogs.CustomPromptDialog;
import util.HttpRequestFormatter;
import util.ThreadManager;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LLMContextMenuProvider implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final LLMClientFactory clientFactory;
    private final ThreadManager threadManager;
    private final SettingsManager settingsManager;
    private final ChatController chatController;

    public LLMContextMenuProvider(MontoyaApi api, LLMClientFactory clientFactory,
                                   ThreadManager threadManager, SettingsManager settingsManager,
                                   ChatController chatController) {
        this.api = api;
        this.clientFactory = clientFactory;
        this.threadManager = threadManager;
        this.settingsManager = settingsManager;
        this.chatController = chatController;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<HttpRequestResponse> selectedItems = event.selectedRequestResponses();
        if (selectedItems.isEmpty()) {
            return List.of();
        }

        JMenu root = new JMenu("AI Pal");

        // Keep options in a single group, ordered alphabetically.
        JMenuItem analyzeItem = new JMenuItem("Analyze for Vulnerabilities");
        analyzeItem.addActionListener(e -> analyzeForVulnerabilities(selectedItems));

        JMenuItem chatItem = new JMenuItem("Chat");
        chatItem.addActionListener(e -> chatController.sendToChat(selectedItems.get(0)));

        JMenuItem customItem = new JMenuItem("Custom Prompt");
        customItem.addActionListener(e -> customPrompt(selectedItems));

        JMenuItem explainItem = new JMenuItem("Explain Request/Response");
        explainItem.addActionListener(e -> explainRequest(selectedItems));

        JMenuItem attackItem = new JMenuItem("Generate Attack Vectors");
        attackItem.addActionListener(e -> generateAttackVectors(selectedItems));

        root.add(analyzeItem);
        root.add(chatItem);
        root.add(customItem);
        root.add(explainItem);
        root.add(attackItem);

        List<Component> menuItems = new ArrayList<>();
        menuItems.add(root);
        return menuItems;
    }

    private void analyzeForVulnerabilities(List<HttpRequestResponse> items) {
        if (!checkConfig()) return;

        Frame parentFrame = api.userInterface().swingUtils().suiteFrame();
        String httpContent = formatItems(items);

        AnalysisResultDialog dialog = AnalysisResultDialog.showLoading(
                parentFrame, "Vulnerability Analysis - " + settingsManager.getActiveProvider().getDisplayName());

        threadManager.submitAsync(() -> {
            LLMClient client = clientFactory.createClient();
            String prompt = PromptTemplates.formatAnalysisPrompt(httpContent);
            LLMResponse response = client.complete(prompt, PromptTemplates.VULNERABILITY_ANALYSIS_SYSTEM);

            SwingUtilities.invokeLater(() -> {
                if (response.isSuccess()) {
                    dialog.setResult(formatResultWithHeader("Vulnerability Analysis", items, response.getContent()));
                } else {
                    dialog.setResult("Error: " + response.getErrorMessage());
                }
            });
        }, error -> SwingUtilities.invokeLater(() ->
                dialog.setResult("Error: " + error.getMessage())
        ));
    }

    private void explainRequest(List<HttpRequestResponse> items) {
        if (!checkConfig()) return;

        Frame parentFrame = api.userInterface().swingUtils().suiteFrame();
        String httpContent = formatItems(items);

        AnalysisResultDialog dialog = AnalysisResultDialog.showLoading(
                parentFrame, "Request Explanation - " + settingsManager.getActiveProvider().getDisplayName());

        threadManager.submitAsync(() -> {
            LLMClient client = clientFactory.createClient();
            String prompt = PromptTemplates.formatExplainPrompt(httpContent);
            LLMResponse response = client.complete(prompt, PromptTemplates.EXPLAIN_REQUEST_SYSTEM);

            SwingUtilities.invokeLater(() -> {
                if (response.isSuccess()) {
                    dialog.setResult(formatResultWithHeader("Request Explanation", items, response.getContent()));
                } else {
                    dialog.setResult("Error: " + response.getErrorMessage());
                }
            });
        }, error -> SwingUtilities.invokeLater(() ->
                dialog.setResult("Error: " + error.getMessage())
        ));
    }

    private void generateAttackVectors(List<HttpRequestResponse> items) {
        if (!checkConfig()) return;

        Frame parentFrame = api.userInterface().swingUtils().suiteFrame();
        String httpContent = formatItems(items);

        AnalysisResultDialog dialog = AnalysisResultDialog.showLoading(
                parentFrame, "Attack Vectors - " + settingsManager.getActiveProvider().getDisplayName());

        threadManager.submitAsync(() -> {
            LLMClient client = clientFactory.createClient();
            String prompt = PromptTemplates.formatAttackVectorsPrompt(httpContent);
            LLMResponse response = client.complete(prompt, PromptTemplates.ATTACK_VECTORS_SYSTEM);

            SwingUtilities.invokeLater(() -> {
                if (response.isSuccess()) {
                    dialog.setResult(formatResultWithHeader("Attack Vectors", items, response.getContent()));
                } else {
                    dialog.setResult("Error: " + response.getErrorMessage());
                }
            });
        }, error -> SwingUtilities.invokeLater(() ->
                dialog.setResult("Error: " + error.getMessage())
        ));
    }

    private void customPrompt(List<HttpRequestResponse> items) {
        if (!checkConfig()) return;

        Frame parentFrame = api.userInterface().swingUtils().suiteFrame();
        String httpContent = formatItems(items);

        String userPrompt = CustomPromptDialog.showDialog(parentFrame, httpContent);
        if (userPrompt == null) return;

        AnalysisResultDialog dialog = AnalysisResultDialog.showLoading(
                parentFrame, "Custom Analysis - " + settingsManager.getActiveProvider().getDisplayName());

        threadManager.submitAsync(() -> {
            LLMClient client = clientFactory.createClient();
            String fullPrompt = userPrompt + "\n\n" + httpContent;
            LLMResponse response = client.complete(fullPrompt, null);

            SwingUtilities.invokeLater(() -> {
                if (response.isSuccess()) {
                    dialog.setResult(formatResultWithHeader("Custom Analysis", items, response.getContent()));
                } else {
                    dialog.setResult("Error: " + response.getErrorMessage());
                }
            });
        }, error -> SwingUtilities.invokeLater(() ->
                dialog.setResult("Error: " + error.getMessage())
        ));
    }

    private boolean checkConfig() {
        if (!clientFactory.hasValidConfig()) {
            Frame parentFrame = api.userInterface().swingUtils().suiteFrame();
            JOptionPane.showMessageDialog(parentFrame,
                    "Please configure the LLM provider in the extension settings first.\n" +
                            "Go to AI Pal > Settings",
                    "Configuration Required",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private String formatItems(List<HttpRequestResponse> items) {
        if (items.size() == 1) {
            return HttpRequestFormatter.format(items.get(0));
        }

        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (HttpRequestResponse item : items) {
            sb.append("=== Request #").append(index++).append(" ===\n");
            sb.append(HttpRequestFormatter.format(item));
            sb.append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Format the analysis result with a header (like Atlas-AI).
     */
    private String formatResultWithHeader(String analysisType, List<HttpRequestResponse> items, String content) {
        StringBuilder sb = new StringBuilder();
        String divider = "============================================================";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sb.append("AI PAL SECURITY ANALYSIS\n");
        sb.append(divider).append("\n");

        // Add target info from first request
        if (!items.isEmpty()) {
            HttpRequestResponse first = items.get(0);
            if (first.request() != null) {
                String url = first.request().url();
                sb.append("Target: ").append(url).append("\n");
            }
        }

        sb.append("Analysis Type: ").append(analysisType).append("\n");
        sb.append("Provider: ").append(settingsManager.getActiveProvider().getDisplayName()).append("\n");
        sb.append("Time: ").append(timestamp).append("\n");
        sb.append(divider).append("\n\n");
        sb.append(content);

        return sb.toString();
    }
}