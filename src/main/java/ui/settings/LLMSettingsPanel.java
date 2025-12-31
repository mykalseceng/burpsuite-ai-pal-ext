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

    private final ButtonGroup providerGroup = new ButtonGroup();
    private final Map<LLMProvider, JRadioButton> providerButtons = new HashMap<>();

    // Ollama-specific fields
    private JTextField ollamaBaseUrlField;
    private JTextField ollamaModelField;
    private JLabel ollamaStatusLabel;
    private JButton ollamaStartButton;
    private JButton ollamaStopButton;

    // Bedrock-specific fields
    private JPasswordField bedrockAccessKeyField;
    private JPasswordField bedrockSecretKeyField;
    private JPasswordField bedrockSessionTokenField;
    private JComboBox<String> bedrockRegionDropdown;

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
        mainPanel.add(createOllamaPanel());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createBedrockPanel());

        // Set initial selection
        LLMProvider activeProvider = settingsManager.getActiveProvider();
        JRadioButton activeButton = providerButtons.get(activeProvider);
        if (activeButton != null) {
            activeButton.setSelected(true);
        }

        add(new JScrollPane(mainPanel), BorderLayout.CENTER);

        // Auto-check connection status on startup
        checkConnectionStatusAsync();
    }

    /**
     * Automatically check connection status when panel loads.
     */
    private void checkConnectionStatusAsync() {
        ollamaStatusLabel.setText("Checking connection...");
        ollamaStatusLabel.setForeground(Color.GRAY);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String statusInfo = "";

            @Override
            protected Boolean doInBackground() {
                try {
                    String baseUrl = settingsManager.getOllamaBaseUrl();
                    // Normalize URL - add http:// if no scheme present
                    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                        baseUrl = "http://" + baseUrl;
                    }
                    java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofSeconds(3))
                            .build();

                    // First check /api/ps to see what models are currently loaded
                    java.net.http.HttpRequest psRequest = java.net.http.HttpRequest.newBuilder()
                            .uri(new java.net.URI(baseUrl + "/api/ps"))
                            .timeout(java.time.Duration.ofSeconds(5))
                            .GET()
                            .build();
                    java.net.http.HttpResponse<String> psResponse = client.send(psRequest,
                            java.net.http.HttpResponse.BodyHandlers.ofString());

                    String loadedModel = null;
                    if (psResponse.statusCode() == 200) {
                        String psBody = psResponse.body();
                        // Parse the running model name from /api/ps response
                        // Format: {"models":[{"name":"llama3.2:latest",...}]}
                        if (psBody.contains("\"name\"")) {
                            int nameStart = psBody.indexOf("\"name\"") + 8;
                            int nameEnd = psBody.indexOf("\"", nameStart);
                            if (nameEnd > nameStart) {
                                loadedModel = psBody.substring(nameStart, nameEnd);
                            }
                        }
                    }

                    // Then check /api/tags for available models count
                    java.net.http.HttpRequest tagsRequest = java.net.http.HttpRequest.newBuilder()
                            .uri(new java.net.URI(baseUrl + "/api/tags"))
                            .timeout(java.time.Duration.ofSeconds(5))
                            .GET()
                            .build();
                    java.net.http.HttpResponse<String> tagsResponse = client.send(tagsRequest,
                            java.net.http.HttpResponse.BodyHandlers.ofString());

                    if (tagsResponse.statusCode() == 200) {
                        String tagsBody = tagsResponse.body();
                        int availableCount = tagsBody.split("\"name\"").length - 1;

                        if (loadedModel != null && !loadedModel.isEmpty()) {
                            statusInfo = "Loaded: " + loadedModel + " (" + availableCount + " available)";
                        } else {
                            statusInfo = "No model loaded (" + availableCount + " available)";
                        }
                        return true;
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        ollamaStatusLabel.setText("Connected - " + statusInfo);
                        ollamaStatusLabel.setForeground(new Color(0, 150, 0)); // Green
                    } else {
                        ollamaStatusLabel.setText("Not connected - Ollama server not reachable");
                        ollamaStatusLabel.setForeground(new Color(200, 0, 0)); // Red
                    }
                } catch (Exception ex) {
                    ollamaStatusLabel.setText("Not connected - " + ex.getMessage());
                    ollamaStatusLabel.setForeground(new Color(200, 0, 0)); // Red
                }
            }
        };
        worker.execute();
    }

    private JPanel createOllamaPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Ollama (Local)",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        // Radio button row
        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JRadioButton radioButton = new JRadioButton("Use Ollama");
        radioButton.addActionListener(e -> {
            if (radioButton.isSelected()) {
                settingsManager.setActiveProvider(LLMProvider.OLLAMA);
            }
        });
        providerGroup.add(radioButton);
        providerButtons.put(LLMProvider.OLLAMA, radioButton);
        radioRow.add(radioButton);
        radioRow.add(new JLabel("- Run models locally with complete privacy"));
        radioRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(radioRow);

        // Base URL row
        JPanel baseUrlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        baseUrlRow.add(new JLabel("Base URL:"));
        ollamaBaseUrlField = new JTextField(30);
        ollamaBaseUrlField.setText(settingsManager.getOllamaBaseUrl());
        ollamaBaseUrlField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            settingsManager.setOllamaBaseUrl(ollamaBaseUrlField.getText());
        }));
        baseUrlRow.add(ollamaBaseUrlField);
        baseUrlRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(baseUrlRow);

        // Model input row
        JPanel modelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        modelRow.add(new JLabel("Model:"));
        ollamaModelField = new JTextField(20);
        ollamaModelField.setText(settingsManager.getModel(LLMProvider.OLLAMA));
        ollamaModelField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            settingsManager.setModel(LLMProvider.OLLAMA, ollamaModelField.getText());
        }));
        modelRow.add(ollamaModelField);

        // Pull model button
        JButton pullButton = new JButton("Pull Model");
        pullButton.addActionListener(e -> pullOllamaModel(pullButton));
        modelRow.add(pullButton);
        modelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(modelRow);

        // Server control row
        JPanel serverRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        serverRow.add(new JLabel("Server:"));

        ollamaStartButton = new JButton("Run Model");
        ollamaStartButton.addActionListener(e -> startOllamaModel());
        serverRow.add(ollamaStartButton);

        ollamaStopButton = new JButton("Stop Model");
        ollamaStopButton.addActionListener(e -> stopOllamaModel());
        serverRow.add(ollamaStopButton);

        // Refresh status button
        JButton refreshButton = new JButton("Refresh Status");
        refreshButton.addActionListener(e -> checkConnectionStatusAsync());
        serverRow.add(refreshButton);

        serverRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(serverRow);

        // Status row
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ollamaStatusLabel = new JLabel(" ");
        ollamaStatusLabel.setFont(ollamaStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        statusRow.add(ollamaStatusLabel);
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(statusRow);

        return panel;
    }

    private String findOllamaExecutable() {
        // Check common installation paths for ollama
        String os = System.getProperty("os.name").toLowerCase();
        String[] possiblePaths;

        if (os.contains("mac")) {
            possiblePaths = new String[]{
                    "/usr/local/bin/ollama",
                    "/opt/homebrew/bin/ollama",
                    System.getProperty("user.home") + "/.local/bin/ollama"
            };
        } else if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            possiblePaths = new String[]{
                    (localAppData != null ? localAppData : "") + "\\Programs\\Ollama\\ollama.exe",
                    "C:\\Program Files\\Ollama\\ollama.exe",
                    System.getProperty("user.home") + "\\AppData\\Local\\Programs\\Ollama\\ollama.exe"
            };
        } else {
            // Linux
            possiblePaths = new String[]{
                    "/usr/local/bin/ollama",
                    "/usr/bin/ollama",
                    System.getProperty("user.home") + "/.local/bin/ollama"
            };
        }

        for (String path : possiblePaths) {
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.canExecute()) {
                return path;
            }
        }

        return null; // Not found
    }

    private boolean checkOllamaInstalled() {
        String ollamaPath = findOllamaExecutable();
        if (ollamaPath == null) {
            JOptionPane.showMessageDialog(this,
                    "Ollama is not installed or not in PATH.\n\n" +
                            "Please install Ollama from: https://ollama.ai/download",
                    "Ollama Not Found",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean isModelAlreadyPulled(String ollamaPath, String modelName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(ollamaPath, "list");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // The first column is the model name (e.g., "llama3:8b" or "llama3.2:latest")
                String[] parts = line.trim().split("\\s+");
                if (parts.length > 0) {
                    String listedModel = parts[0].toLowerCase();
                    String searchModel = modelName.toLowerCase();

                    // Check for exact match or match without tag
                    if (listedModel.equals(searchModel) ||
                            listedModel.startsWith(searchModel + ":") ||
                            (searchModel.contains(":") && listedModel.equals(searchModel.split(":")[0] + ":latest"))) {
                        return true;
                    }
                    // Also check if user specified model without tag and it matches with :latest
                    if (!searchModel.contains(":") && listedModel.equals(searchModel + ":latest")) {
                        return true;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // If we can't check, assume not pulled
        }
        return false;
    }

    private void pullOllamaModel(JButton button) {
        if (!checkOllamaInstalled()) {
            return;
        }

        String model = ollamaModelField.getText().trim();
        if (model.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a model name to pull.",
                    "Model Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ollamaPath = findOllamaExecutable();

        // Check if model is already pulled
        if (isModelAlreadyPulled(ollamaPath, model)) {
            ollamaStatusLabel.setText("Model '" + model + "' is already available.");
            JOptionPane.showMessageDialog(this,
                    "Model '" + model + "' is already pulled.\n\n" +
                            "Click 'Run Model' to start using it.",
                    "Model Available",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        button.setEnabled(false);
        button.setText("Pulling...");
        ollamaStatusLabel.setText("Pulling model: " + model + "...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(ollamaPath, "pull", model);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        errorMessage = "Pull failed with exit code: " + exitCode;
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                button.setText("Pull Model");
                try {
                    if (get()) {
                        ollamaStatusLabel.setText("Model '" + model + "' pulled successfully.");
                        JOptionPane.showMessageDialog(LLMSettingsPanel.this,
                                "Model '" + model + "' pulled successfully!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        ollamaStatusLabel.setText("Failed to pull model.");
                        JOptionPane.showMessageDialog(LLMSettingsPanel.this,
                                "Failed to pull model:\n" + errorMessage +
                                        "\n\nMake sure Ollama is installed and the server is running.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ollamaStatusLabel.setText("Failed to pull model.");
                    JOptionPane.showMessageDialog(LLMSettingsPanel.this,
                            "Failed to pull model:\n" + ex.getMessage() +
                                    "\n\nMake sure Ollama is installed and the server is running.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void startOllamaModel() {
        if (!checkOllamaInstalled()) {
            return;
        }

        String model = ollamaModelField.getText().trim();
        if (model.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a model name to run.",
                    "Model Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ollamaPath = findOllamaExecutable();
        ollamaStartButton.setEnabled(false);
        ollamaStatusLabel.setText("Starting model: " + model + "...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    // Use 'ollama run' which starts the model (and pulls if needed)
                    // Run in background with keepalive to keep it loaded
                    ProcessBuilder pb = new ProcessBuilder(ollamaPath, "run", model, "--keepalive", "24h");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    // Send empty input and close to exit interactive mode but keep model loaded
                    process.getOutputStream().close();

                    // Wait for process to initialize (it will keep model loaded due to keepalive)
                    Thread.sleep(3000);
                    return true;
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                ollamaStartButton.setEnabled(true);
                try {
                    if (get()) {
                        // Refresh connection status to show updated state
                        checkConnectionStatusAsync();
                    } else {
                        ollamaStatusLabel.setText("Failed to start model: " + errorMessage);
                        ollamaStatusLabel.setForeground(new Color(200, 0, 0));
                    }
                } catch (Exception ex) {
                    ollamaStatusLabel.setText("Failed to start model: " + ex.getMessage());
                    ollamaStatusLabel.setForeground(new Color(200, 0, 0));
                }
            }
        };
        worker.execute();
    }

    private void stopOllamaModel() {
        if (!checkOllamaInstalled()) {
            return;
        }

        String model = ollamaModelField.getText().trim();
        if (model.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a model name to stop.",
                    "Model Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ollamaPath = findOllamaExecutable();
        ollamaStopButton.setEnabled(false);
        ollamaStatusLabel.setText("Stopping model: " + model + "...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage;

            @Override
            protected Boolean doInBackground() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(ollamaPath, "stop", model);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        errorMessage = "Stop failed with exit code: " + exitCode;
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                ollamaStopButton.setEnabled(true);
                try {
                    if (get()) {
                        // Refresh connection status to show updated state
                        checkConnectionStatusAsync();
                    } else {
                        ollamaStatusLabel.setText("Failed to stop model: " + errorMessage);
                        ollamaStatusLabel.setForeground(new Color(200, 0, 0));
                    }
                } catch (Exception ex) {
                    ollamaStatusLabel.setText("Failed to stop model: " + ex.getMessage());
                    ollamaStatusLabel.setForeground(new Color(200, 0, 0));
                }
            }
        };
        worker.execute();
    }

    private JPanel createBedrockPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "AWS Bedrock",
                TitledBorder.LEFT,
                TitledBorder.TOP
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        // Radio button row
        JPanel radioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JRadioButton radioButton = new JRadioButton("Use AWS Bedrock");
        radioButton.addActionListener(e -> {
            if (radioButton.isSelected()) {
                settingsManager.setActiveProvider(LLMProvider.BEDROCK);
            }
        });
        providerGroup.add(radioButton);
        providerButtons.put(LLMProvider.BEDROCK, radioButton);
        radioRow.add(radioButton);
        radioRow.add(new JLabel("- Access Claude models via AWS Bedrock"));
        radioRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(radioRow);

        // Region row
        JPanel regionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        regionRow.add(new JLabel("Region:"));
        bedrockRegionDropdown = new JComboBox<>(LLMSettings.BEDROCK_REGIONS);
        bedrockRegionDropdown.setSelectedItem(settingsManager.getBedrockRegion());
        bedrockRegionDropdown.addActionListener(e -> {
            String selected = (String) bedrockRegionDropdown.getSelectedItem();
            if (selected != null) {
                settingsManager.setBedrockRegion(selected);
            }
        });
        regionRow.add(bedrockRegionDropdown);
        regionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(regionRow);

        // Access Key row
        JPanel accessKeyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        accessKeyRow.add(new JLabel("Access Key:"));
        bedrockAccessKeyField = new JPasswordField(35);
        bedrockAccessKeyField.setText(settingsManager.getBedrockAccessKey());
        bedrockAccessKeyField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            settingsManager.setBedrockAccessKey(new String(bedrockAccessKeyField.getPassword()));
        }));
        accessKeyRow.add(bedrockAccessKeyField);
        accessKeyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(accessKeyRow);

        // Secret Key row
        JPanel secretKeyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        secretKeyRow.add(new JLabel("Secret Key:"));
        bedrockSecretKeyField = new JPasswordField(35);
        bedrockSecretKeyField.setText(settingsManager.getBedrockSecretKey());
        bedrockSecretKeyField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            settingsManager.setBedrockSecretKey(new String(bedrockSecretKeyField.getPassword()));
        }));
        secretKeyRow.add(bedrockSecretKeyField);
        secretKeyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(secretKeyRow);

        // Session Token row (optional, for temporary credentials)
        JPanel sessionTokenRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        sessionTokenRow.add(new JLabel("Session Token:"));
        bedrockSessionTokenField = new JPasswordField(35);
        bedrockSessionTokenField.setText(settingsManager.getBedrockSessionToken());
        bedrockSessionTokenField.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            settingsManager.setBedrockSessionToken(new String(bedrockSessionTokenField.getPassword()));
        }));
        sessionTokenRow.add(bedrockSessionTokenField);
        sessionTokenRow.add(new JLabel("(optional)"));
        sessionTokenRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sessionTokenRow);

        // Security warning row
        JPanel warningRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JLabel warningLabel = new JLabel("Note: Credentials stored in Burp preferences. Use env vars or ~/.aws/credentials for better security.");
        warningLabel.setFont(warningLabel.getFont().deriveFont(Font.ITALIC, 10f));
        warningLabel.setForeground(new Color(150, 100, 0)); // Dark orange/warning color
        warningRow.add(warningLabel);
        warningRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(warningRow);

        // Model selection row
        JPanel modelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        modelRow.add(new JLabel("Model:"));
        String[] models = LLMSettings.getModelsForProvider(LLMProvider.BEDROCK);
        JComboBox<String> modelDropdown = new JComboBox<>(models);
        modelDropdown.setSelectedItem(settingsManager.getModel(LLMProvider.BEDROCK));
        modelDropdown.addActionListener(e -> {
            String selected = (String) modelDropdown.getSelectedItem();
            if (selected != null) {
                settingsManager.setModel(LLMProvider.BEDROCK, selected);
            }
        });
        modelRow.add(modelDropdown);

        // Test connection button
        JButton testButton = new JButton("Test Connection");
        testButton.addActionListener(e -> testConnection(LLMProvider.BEDROCK, testButton));
        modelRow.add(testButton);
        modelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(modelRow);

        return panel;
    }

    private void testConnection(LLMProvider provider, JButton button) {
        if (!clientFactory.hasValidConfig(provider)) {
            String message = switch (provider) {
                case OLLAMA -> "Please enter a valid Ollama base URL.";
                case BEDROCK -> "Please configure AWS credentials (in settings or environment variables).";
            };
            JOptionPane.showMessageDialog(this,
                    message,
                    "Configuration Required",
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
