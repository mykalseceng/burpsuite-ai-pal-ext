# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

AI Pal is a Burp Suite extension that integrates LLMs (Ollama, AWS Bedrock) for security analysis, vulnerability detection, and HTTP traffic understanding.

## Key Development Commands

```bash
./gradlew jar      # Build the extension JAR (output: build/libs/burpsuite-ai-pal-ext.jar)
./gradlew build    # Build and test
./gradlew clean    # Clean build artifacts
```

**Quick reload in Burp:** Ctrl/Cmd + click the Loaded checkbox next to AI Pal

## Architecture

### Central API Access Pattern
All Montoya API access goes through `base.Api`:
```java
import static base.Api.api;
// Usage: api.logging(), api.http(), api.userInterface()
```

### Main Components

- **Extension.java** - Entry point, registers all UI components and handlers
- **base/Api.java** - Static holder for MontoyaApi, extensionName, version
- **config/** - Settings persistence (SettingsManager), provider enum (LLMProvider)
- **llm/** - LLM client interface and implementations
  - `LLMClient` - Interface with streaming support
  - `impl/OllamaClient` - Local Ollama with streaming via HttpURLConnection
  - `impl/BedrockClient` - AWS Bedrock via Montoya HTTP API
- **ui/** - All Swing UI components
  - `AIPalSuiteTab` - Main tab container with Chat/Tasks/Settings sub-tabs
  - `chat/` - Chat interface with session management
  - `contextmenu/` - Right-click menu provider
  - `editor/` - Request/Response editor tabs
  - `settings/` - LLM provider configuration panel
  - `tasks/` - Task tracking dashboard

### Key Patterns

- **Static imports** for Api access: `import static base.Api.api`
- **Switch expressions** for enums: `String role = switch (msg.getRole()) { case USER -> "user"; ... }`
- **SwingWorker** for background operations in settings panel
- **StreamCallback** interface for real-time streaming responses
- **ThreadManager** for async LLM operations

### UI Font Standard
All text areas use: `new Font(Font.MONOSPACED, Font.PLAIN, 13)`

## Documentation

- `docs/bapp-store-requirements.md` - BApp Store submission criteria
- `docs/montoya-api-examples.md` - API patterns and examples
- `docs/development-best-practices.md` - AI and threading guidelines
- `docs/resources.md` - External documentation links

## Build Configuration

- Java 21, Montoya API 2025.10, Gson 2.10.1
- JAR bundles all runtime dependencies (Gson)
