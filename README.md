# AI Pal - Burp Suite Extension

AI Pal is a Burp Suite extension that integrates Large Language Models (LLMs) to assist security researchers with vulnerability analysis, attack vector generation, and HTTP traffic understanding.

## Features

### Multi-Provider LLM Support

AI Pal supports four LLM providers:

- **Ollama** - Run models locally with complete privacy (Llama, Mistral, DeepSeek, and more)
- **AWS Bedrock** - Enterprise-grade access to Claude models via AWS infrastructure
- **Claude Code CLI** - Run prompts through your local `claude` CLI
- **OpenAI Codex CLI** - Run prompts through your local `codex` CLI

Switch between providers in the Settings tab with automatic credential or binary-path detection.

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
- Streaming responses for real-time output (Ollama, Claude Code CLI, and OpenAI Codex CLI)
- Send HTTP requests/responses directly to chat from context menu
- Ask follow-up questions about vulnerabilities
- Get help with payload crafting and testing strategies

### Editor Integration

AI Pal adds an "AI Pal" tab to the HTTP request and response editors in Repeater and other tools:

- Write custom prompts with attached request/response context
- Toggle which context to include (Request, Response, Notes)
- Track analysis tasks in the Tasks tab
- Results appear without leaving your workflow

### Tasks Dashboard

Track all AI analysis operations in a dedicated tab:

- View status of running analyses (pending, running, completed, failed)
- Review past analysis results with full context
- See the prompt, result, and original request/response for each task

## Prerequisites

Before using AI Pal, you need to set up at least one LLM provider:

### Option 1: Ollama (Recommended for Privacy)

Ollama runs models locally on your machine - no data leaves your computer.

1. **Install Ollama** from [ollama.ai](https://ollama.ai/download)
2. **Pull a model**:
   ```bash
   ollama pull llama3.2
   ```
3. **Start the Ollama server** (usually starts automatically):
   ```bash
   ollama serve
   ```
4. **Verify it's running**:
   ```bash
   curl http://localhost:11434/api/tags
   ```

**Recommended models for security analysis:**
| Model | Size | Best For |
|-------|------|----------|
| `llama3.2` | 3B | Fast general analysis |
| `llama3.1:8b` | 8B | Better reasoning |
| `mistral` | 7B | Good balance of speed/quality |
| `deepseek-r1` | 7B+ | Strong reasoning with thinking output |
| `codellama` | 7B | Code-focused analysis |

### Option 2: AWS Bedrock (Enterprise)

AWS Bedrock provides access to Claude models with enterprise security and compliance.

**Requirements:**
- AWS account with Bedrock access enabled in your region
- IAM permissions for `bedrock:InvokeModel`
- Model access granted in the AWS Bedrock console

**Configure credentials using one of these methods (in priority order):**

1. **Environment variables**:
   ```bash
   export AWS_ACCESS_KEY_ID=your_access_key
   export AWS_SECRET_ACCESS_KEY=your_secret_key
   # Optional for temporary credentials:
   export AWS_SESSION_TOKEN=your_session_token
   ```

2. **AWS credentials file** (`~/.aws/credentials`):
   ```ini
   [default]
   aws_access_key_id = your_access_key
   aws_secret_access_key = your_secret_key

   # Or use a named profile:
   [my-profile]
   aws_access_key_id = your_access_key
   aws_secret_access_key = your_secret_key
   ```
   Set `AWS_DEFAULT_PROFILE=my-profile` to use a named profile.

3. **Manual entry** in AI Pal settings (credentials stored in Burp preferences)

**Available models (Global Inference Profiles):**
| Model | Best For |
|-------|----------|
| `claude-sonnet-4-5` | Complex security analysis |
| `claude-sonnet-4` | Balanced performance |
| `claude-haiku-4-5` | Fast, cost-effective |
| `claude-opus-4-5` | Most capable |

**Supported regions:** us-east-1, us-west-2, eu-west-1, eu-central-1, ap-southeast-1, ap-northeast-1

### Option 3: Claude Code CLI

1. Install Claude Code CLI (recommended):
   ```bash
   curl -fsSL https://claude.ai/install.sh | bash
   ```
   npm fallback (deprecated by Claude docs):
   ```bash
   npm install -g @anthropic-ai/claude-code
   ```
2. In AI Pal Settings, select **Use Claude Code**
3. Click **Detect** (or set the binary path manually)
4. Click **Test Connection** to run `claude --version` and verify the binary executes

### Option 4: OpenAI Codex CLI

1. Install OpenAI Codex CLI:
   ```bash
   npm install -g @openai/codex
   ```
2. In AI Pal Settings, select **Use OpenAI Codex**
3. Click **Detect** (or set the binary path manually)
4. Click **Test Connection** to run `codex --version` and verify the binary executes

## Installation

### Building from Source

```bash
# Clone the repository
git clone https://github.com/mykalseceng/burpsuite-ai-pal-ext.git
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

1. After loading the extension, open the **AI Pal** tab in Burp
2. Navigate to the **Settings** sub-tab
3. Select your preferred LLM provider (Ollama, Claude Code CLI, AWS Bedrock, or OpenAI Codex CLI)
4. The settings panel shows your credential status:
   - For Ollama: Connection status and loaded model
   - For Bedrock: Which credential source is being used
   - For Claude/Codex: Detected local CLI path
5. Choose a model and region (for Bedrock)
6. Click **Test Connection** to verify your setup
   - For Claude/Codex this executes `<binary> --version` and requires a runnable executable

## Usage

### Quick Analysis (Context Menu)

1. Capture traffic in Proxy or navigate to any request in Burp
2. Right-click on the request/response
3. Select **AI Pal > Analyze for Vulnerabilities** (or other options)
4. Review the analysis in the popup dialog

### Interactive Chat

1. Go to the **AI Pal > Chat** tab
2. Type your question and press Enter (or click Send)
3. For context, right-click a request and select **AI Pal > Chat**
4. Example questions:
   - "What parameters are most likely vulnerable?"
   - "Generate SQLi payloads for the id parameter"
   - "Explain the authentication flow in this request"

### Editor Integration (AI Pal Tab)

1. Open a request in Repeater or click on any request
2. In the request/response viewer, select the **AI Pal** tab
3. Write your prompt in the text area
4. Toggle context: **Request** | **Response** | **Notes**
5. Click **Analyze**
6. View results in the **Tasks** tab

### Tasks Tab

- Lists all AI analysis tasks with status icons:
  - ⏳ Pending
  - ▶ Running
  - ✓ Completed
  - ! Failed
- Click a task to see full details: prompt, result, and original context

## Project Structure

```
src/main/java/
├── Extension.java           # Main entry point
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
│   │   ├── BedrockClient.java
│   │   ├── ClaudeCodeClient.java
│   │   └── CodexClient.java
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
    ├── AwsCredentialsUtil.java
    ├── CliEnvironmentUtil.java
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

## Security Considerations

- **Ollama**: All data stays local - no external API calls
- **AWS Bedrock**: Data is sent to AWS; follows your organization's AWS security policies
- AWS credentials from environment variables or credentials file are preferred over manual entry
- Manual credentials entered in settings are stored in Burp preferences (not encrypted)
- All LLM requests go through Burp's HTTP stack, respecting proxy settings
- HTTP content sent to LLMs may contain sensitive data - review before sending

## Requirements

- Burp Suite 2024.1+ (uses Montoya API 2025.10)
- Java 21
- **For Ollama**: Local Ollama installation with at least one model pulled
- **For AWS Bedrock**: AWS account with Bedrock model access enabled

## Troubleshooting

### Ollama not connecting
- Ensure Ollama is running: `ollama serve`
- Check the base URL (default: `http://localhost:11434`)
- Verify a model is pulled: `ollama list`

### AWS Bedrock signature errors
- Verify your credentials are valid: `aws sts get-caller-identity`
- Ensure you have Bedrock model access in the AWS console
- Check the region matches where you have access

### Extension not loading
- Ensure Java 21+ is installed
- Check Burp's Extensions > Errors tab for details

## License

MIT License

## Acknowledgments

- Built using the [Burp Montoya API](https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/MontoyaApi.html)
