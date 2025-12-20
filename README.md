# AI Pal - Burp Suite Extension

AI Pal is a Burp Suite extension that integrates Large Language Models (LLMs) to assist security researchers with vulnerability analysis, attack vector generation, and HTTP traffic understanding.

## Features

### Multi-Provider LLM Support

AI Pal supports three major LLM providers:

- **OpenAI** - GPT-4o, GPT-4o Mini, and newer models
- **Google Gemini** - Gemini Flash and Pro models
- **Anthropic Claude** - Claude Opus, Sonnet, and Haiku models

Switch between providers seamlessly in the Settings tab with model selection for each provider.

### Context Menu Actions

Right-click on any request/response in Burp to access AI-powered analysis:

| Action | Description |
|--------|-------------|
| **Analyze for Vulnerabilities** | Comprehensive security analysis covering OWASP Top 10, including SQLi, XSS, CSRF, SSRF, XXE, IDOR, and more |
| **Explain Request/Response** | Plain-English breakdown of what the HTTP traffic does |
| **Generate Attack Vectors** | Produces specific test payloads with bypass techniques |
| **Custom Prompt** | Send your own prompt with the HTTP content as context |
| **Chat** | Opens the request in the Chat tab for interactive analysis |

### Interactive Chat

A built-in chat interface for conversational security analysis:

- Multi-turn conversations with conversation history
- Send HTTP requests/responses directly to chat from context menu
- Ask follow-up questions about vulnerabilities
- Get help with payload crafting and testing strategies

### Editor Integration

AI Pal adds an "AI Security Assistant" tab to the HTTP request and response editors in Repeater and other tools:

- Analyze requests directly from the editor
- Track analysis tasks in the AI Tasks tab
- Results appear inline without leaving your workflow

### AI Tasks Dashboard

Track all AI analysis operations in a dedicated tab:

- View status of running analyses
- Review past analysis results
- Manage concurrent AI operations

## Installation

### Prerequisites

- Burp Suite Professional or Community Edition
- Java 21 or higher
- API key from at least one supported provider (OpenAI, Google, or Anthropic)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/ai-pal.git
cd ai-pal

# Build the extension
./gradlew jar

# The JAR file is created at build/libs/llm-security-assistant.jar
```

### Loading into Burp Suite

1. In Burp, go to **Extensions > Installed**
2. Click **Add**
3. Under **Extension details**, click **Select file**
4. Select `build/libs/llm-security-assistant.jar`
5. Click **Next**

## Configuration

1. After loading the extension, open the **AI Pal** tab
2. Navigate to the **Settings** sub-tab
3. Select your preferred LLM provider
4. Enter your API key for that provider
5. Choose a model (each provider has multiple options)
6. Click **Test Connection** to verify your setup

### API Keys

- **OpenAI**: Get from [platform.openai.com](https://platform.openai.com/api-keys)
- **Google Gemini**: Get from [Google AI Studio](https://aistudio.google.com/apikey)
- **Anthropic Claude**: Get from [console.anthropic.com](https://console.anthropic.com/)

## Usage

### Quick Analysis

1. Capture traffic in Proxy or navigate to any request in Burp
2. Right-click on the request/response
3. Select **AI Pal > Analyze for Vulnerabilities**
4. Review the analysis in the popup dialog

### Interactive Chat

1. Right-click a request and select **AI Pal > Chat**
2. The request is loaded into the Chat tab
3. Ask questions like:
   - "What parameters are most likely vulnerable?"
   - "Generate XSS payloads for the search parameter"
   - "Explain the authentication flow"

### Editor Integration

1. Open a request in Repeater
2. Select the **AI Security Assistant** tab
3. Use the analysis buttons to examine the request
4. View your task in the **AI Tasks** sub-tab

## Project Structure

```
src/main/java/
├── Extension.java          # Main entry point
├── config/                  # Settings and provider configuration
│   ├── LLMProvider.java
│   ├── LLMSettings.java
│   └── SettingsManager.java
├── llm/                     # LLM client implementations
│   ├── LLMClient.java
│   ├── LLMClientFactory.java
│   ├── LLMResponse.java
│   ├── impl/
│   │   ├── ClaudeClient.java
│   │   ├── GeminiClient.java
│   │   └── OpenAIClient.java
│   └── prompts/
│       └── PromptTemplates.java
├── ui/                      # User interface components
│   ├── AIPalSuiteTab.java
│   ├── chat/
│   ├── contextmenu/
│   ├── dialogs/
│   ├── editor/
│   ├── settings/
│   └── tasks/
└── util/                    # Utilities
    ├── HttpRequestFormatter.java
    ├── ThreadManager.java
    └── Utf16Sanitizer.java
```

## Development

### Build Commands

```bash
./gradlew build    # Build and run tests
./gradlew jar      # Create the extension JAR
./gradlew clean    # Clean build artifacts
```

### Quick Reload During Development

1. Make code changes
2. Run `./gradlew jar`
3. In Burp: Hold **Ctrl/Cmd** and click the **Loaded** checkbox next to AI Pal

### Adding a New LLM Provider

1. Add the provider to `config/LLMProvider.java`
2. Add models to `config/LLMSettings.java`
3. Create a new client in `llm/impl/`
4. Register in `LLMClientFactory.java`

## Security Considerations

- API keys are stored in Burp's preferences (encrypted by Burp)
- All LLM requests go through Burp's HTTP stack, respecting proxy settings
- HTTP content sent to LLMs may contain sensitive data - review before sending

## Requirements

- Burp Suite 2024.1+ (uses Montoya API 2025.10)
- Java 21
- Internet access to LLM provider APIs

## License

MIT License

## Acknowledgments

- Built using the [Burp Montoya API](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html)
- Inspired by the PortSwigger extension template
