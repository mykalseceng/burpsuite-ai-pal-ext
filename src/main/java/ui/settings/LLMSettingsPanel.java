package ui.settings;

import config.LLMProvider;
import config.LLMSettings;
import config.SettingsManager;
import llm.LLMClientFactory;
import llm.LLMResponse;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LLMSettingsPanel extends JPanel {
    private final SettingsManager settingsManager;
    private final LLMClientFactory clientFactory;

    private final Map<LLMProvider, JPasswordField> apiKeyFields = new HashMap<>();
    private final Map<LLMProvider, JComboBox<String>> modelDropdowns = new HashMap<>();
    private final ButtonGroup providerGroup = new ButtonGroup();
    private final Map<LLMProvider, JRadioButton> providerButtons = new HashMap<>();

    public LLMSettingsPanel(SettingsManager settingsManager, LLMClientFactory clientFactory) {
        this.settingsManager = settingsManager;
        this.clientFactory = clientFactory;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Title
        JLabel titleLabel = new JLabel("LLM Security Assistant Settings");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        mainPanel.add(titleLabel);

        // Provider panels
        for (LLMProvider provider : LLMProvider.values()) {
            mainPanel.add(createProviderPanel(provider));
            mainPanel.add(Box.createVerticalStrut(10));
        }

        // Set initial selection
        LLMProvider activeProvider = settingsManager.getActiveProvider();
        JRadioButton activeButton = providerButtons.get(activeProvider);
        if (activeButton != null) {
            activeButton.setSelected(true);
        }

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);
    }

    private JPanel createProviderPanel(LLMProvider provider) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                provider.getDisplayName(),
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        // Radio button row
        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JRadioButton radioButton = new JRadioButton("Use " + provider.getDisplayName());
        radioButton.addActionListener(e -> {
            if (radioButton.isSelected()) {
                settingsManager.setActiveProvider(provider);
            }
        });
        providerGroup.add(radioButton);
        providerButtons.put(provider, radioButton);
        radioRow.add(radioButton);
        radioRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(radioRow);

        // API Key row
        JPanel apiKeyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        apiKeyRow.add(new JLabel("API Key:"));
        JPasswordField apiKeyField = new JPasswordField(40);
        apiKeyField.setText(settingsManager.getApiKey(provider));
        apiKeyField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            settingsManager.setApiKey(provider, new String(apiKeyField.getPassword()));
        }));
        apiKeyFields.put(provider, apiKeyField);
        apiKeyRow.add(apiKeyField);
        apiKeyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(apiKeyRow);

        // Model selection row
        JPanel modelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        modelRow.add(new JLabel("Model:"));
        String[] models = LLMSettings.getModelsForProvider(provider);
        JComboBox<String> modelDropdown = new JComboBox<>(models);
        modelDropdown.setSelectedItem(settingsManager.getModel(provider));
        modelDropdown.addActionListener(e -> {
            String selected = (String) modelDropdown.getSelectedItem();
            if (selected != null) {
                settingsManager.setModel(provider, selected);
            }
        });
        modelDropdowns.put(provider, modelDropdown);
        modelRow.add(modelDropdown);

        // Test connection button
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection(provider, testButton));
        modelRow.add(testButton);
        modelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(modelRow);

        return panel;
    }

    private void testConnection(LLMProvider provider, JButton button) {
        if (!clientFactory.hasValidApiKey(provider)) {
            JOptionPane.showMessageDialog(this,
                    "Please enter an API key first.",
                    "No API Key",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        button.setEnabled(false);
        button.setText("Testing...");

        SwingWorker<LLMResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected LLMResponse doInBackground() {
                return clientFactory.createClient(provider).complete("Say OK", null);
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                button.setText("Test Connection");
                try {
                    LLMResponse response = get();
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(LLMSettingsPanel.this,
                                "Connection successful!\n\nResponse: " +
                                        (response.getContent().length() > 100 ?
                                                response.getContent().substring(0, 100) + "..." :
                                                response.getContent()),
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(LLMSettingsPanel.this,
                                "Connection failed:\n" + response.getErrorMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LLMSettingsPanel.this,
                            "Connection failed:\n" + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Simple document listener helper
    private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final Runnable action;

        SimpleDocumentListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }
    }
}