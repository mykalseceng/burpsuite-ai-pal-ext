# AI Pal - Burp Suite Extension

AI Pal is a Burp Suite extension that integrates Large Language Models (LLMs) to assist security researchers with vulnerability analysis, attack vector generation, and HTTP traffic understanding.

## Features

### Multi-Provider LLM Support

AI Pal supports two LLM providers:

- **Ollama** - Run models locally with complete privacy (Llama, Mistral, CodeLlama, and more)
- **AWS Bedrock** - Enterprise-grade access to Claude, Llama, and other foundation models

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
- One of the following:
  - **Ollama** installed locally ([ollama.ai](https://ollama.ai))
  - **AWS account** with Bedrock access and configured credentials

### Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/burpsuite-ai-pal-ext.git
cd burpsuite-ai-pal-ext

# Build the extension
./gradlew jar

# The JAR file is created at build/libs/burpsuite-ai-pal-ext.jar
```

### Loading into Burp Suite

1. In Burp, go to **Extensions > Installed**
2. Click **Add**
3. Under **Extension details**, click **Select file**
4. Select `build/libs/burpsuite-ai-pal-ext.jar`
5. Click **Next**

## Configuration

1. After loading the extension, open the **AI Pal** tab
2. Navigate to the **Settings** sub-tab
3. Select your preferred LLM provider
4. Configure the provider settings (see below)
5. Choose a model
6. Click **Test Connection** to verify your setup

### Ollama Setup

1. Install Ollama from [ollama.ai](https://ollama.ai)
2. Pull a model: `ollama pull llama3.2` (or your preferred model)
3. Ensure Ollama is running (default: `http://localhost:11434`)
4. In AI Pal settings, select **Ollama** and enter the base URL

**Recommended models for security analysis:**
- `llama3.2` - Good balance of speed and capability
- `mistral` - Fast and capable
- `codellama` - Optimized for code analysis
- `deepseek-coder` - Strong at code understanding

### AWS Bedrock Setup

1. Ensure you have an AWS account with Bedrock access enabled
2. Configure AWS credentials using one of these methods:
   - Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
   - AWS credentials file (`~/.aws/credentials`)
   - IAM role (when running on AWS infrastructure)
3. In AI Pal settings, select **AWS Bedrock** and choose your region

**Available models:**
- `anthropic.claude-3-5-sonnet-20241022-v2:0` - Best for complex analysis
- `anthropic.claude-3-5-haiku-20241022-v1:0` - Fast and cost-effective
- `meta.llama3-70b-instruct-v1:0` - Open-source alternative
- `amazon.titan-text-premier-v1:0` - AWS native model

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
│   │   ├── OllamaClient.java
│   │   └── BedrockClient.java
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

- **Ollama**: All data stays local - no external API calls
- **AWS Bedrock**: Data is sent to AWS; follows your organization's AWS security policies
- AWS credentials are managed via standard AWS SDK credential chain (not stored by the extension)
- All LLM requests go through Burp's HTTP stack, respecting proxy settings
- HTTP content sent to LLMs may contain sensitive data - review before sending

## Requirements

- Burp Suite 2024.1+ (uses Montoya API 2025.10)
- Java 21
- For Ollama: Local Ollama installation with at least one model
- For AWS Bedrock: AWS credentials with Bedrock access and internet connectivity

## License

MIT License

## Acknowledgments

- Built using the [Burp Montoya API](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html)
- Inspired by the PortSwigger extension template
