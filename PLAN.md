# Plan: Claude Code CLI Integration

## Context
Add Claude Code as a third LLM provider in AI Pal. Users invoke their locally installed `claude` CLI (authenticated with their own subscription) via subprocess. No API keys needed — uses existing Claude Code login.

## Approach
Shell out to `claude -p "prompt" --output-format stream-json --allowedTools ""` via `ProcessBuilder`.

## Security: No Tool Access
**CRITICAL:** The `claude` CLI is an agent with access to Bash, Read, Write, Edit, etc. AI Pal feeds untrusted web content (HTTP requests/responses from potentially malicious sites) into prompts. A prompt injection attack in HTTP traffic could trick Claude into executing arbitrary commands.

**Mitigation:** Always invoke with `--allowedTools ""` to disable ALL tools. This makes Claude Code behave as a pure text-in/text-out LLM — identical to how Ollama and Bedrock providers work. No file access, no shell commands, no tool use.

Additionally:
- Never pass `--dangerously-skip-permissions` or similar flags
- Pipe prompt via stdin (not shell argument) to avoid shell injection
- Sanitize/escape prompt content before passing to process

## Files to Modify

### 1. `config/LLMProvider.java`
- Add `CLAUDE_CODE("Claude Code", "localhost")` enum constant

### 2. `config/LLMSettings.java`
- Add model list for CLAUDE_CODE:
  - `claude-sonnet-4-6` (default)
  - `claude-opus-4-6`
  - `claude-haiku-4-5-20251001`

### 3. `config/SettingsManager.java`
- Add persistence keys: `llm.claudecode.path`, `llm.claudecode.model`
- Add getters/setters: `getClaudeCodePath()`, `setClaudeCodePath()`
- Default path: auto-detect via `which claude` or common locations

### 4. `llm/impl/ClaudeCodeClient.java` (NEW)
- Implement `LLMClient` interface
- `complete()` / `chat()`: invoke `claude -p` via ProcessBuilder, parse JSON response
- `chatStreaming()`: use `--output-format stream-json`, read stdout line-by-line, parse streaming JSON chunks
- `supportsStreaming()`: return `true`
- `testConnection()`: run `claude --version`, check exit code
- `getProviderName()`: return "Claude Code"
- Build conversation context by concatenating message history into the prompt
- Handle process timeout (configurable, default 120s)
- Respect `StreamCallback.isCancelled()` by destroying the process

### 5. `llm/LLMClientFactory.java`
- Add `case CLAUDE_CODE ->` in `createClient()` switch
- Add config validation in `hasValidConfig()` — check claude binary exists

### 6. `ui/settings/LLMSettingsPanel.java`
- Add Claude Code settings panel section (radio button, path field, model dropdown, test button)
- Auto-detect CLI path on panel load
- Show version + auth status on connection test
- "Detect Path" button to find `claude` binary

## CLI Invocation Details

**Non-streaming (pipe via stdin):**
```bash
echo "prompt" | claude -p --output-format json --model claude-sonnet-4-6 --allowedTools ""
```

**Streaming (pipe via stdin):**
```bash
echo "prompt" | claude -p --output-format stream-json --model claude-sonnet-4-6 --allowedTools ""
```

**Connection test:**
```bash
claude --version
```

**Process management:**
- Use `ProcessBuilder` with redirected stderr
- Set working directory to temp or user home
- Read stdout for response, stderr for errors
- Destroy process on cancel/timeout

## Conversation History
The `claude -p` flag is single-turn. To support multi-turn chat:
- Concatenate conversation history into a single prompt with role markers
- Format: `User: ...\nAssistant: ...\nUser: ...`
- Pass as single prompt to `claude -p`

## Settings UI Layout
```
○ Use Claude Code
  - Run prompts via your local Claude Code CLI installation

  Path:  [/usr/local/bin/claude    ] [Detect]
  Model: [sonnet ▾]
  [Test Connection]
  Status: Connected - Claude Code v1.x.x
```

## Verification
1. `./gradlew jar` — builds without errors
2. Load extension in Burp Suite
3. Settings tab: select Claude Code provider, detect path, test connection
4. Chat tab: send a message, verify streaming response
5. Context menu: right-click request → Analyze for Vulnerabilities
6. Test cancel mid-stream (stop button)
7. Test with invalid/missing `claude` binary — should show clear error

## Open Questions
- Should we support session resumption (`--resume`) for chat continuity instead of prompt concatenation?
- Max timeout value — configurable in settings or hardcoded?
