# Plan: OpenAI Codex CLI Integration

## Context
Add OpenAI Codex as a fourth LLM provider in AI Pal. Users invoke their locally installed `codex` CLI (authenticated with their ChatGPT subscription or API key) via subprocess. Uses `codex exec` for headless, non-interactive prompts.

## Approach
Shell out to `codex exec --json --ephemeral --full-auto --sandbox read-only -m gpt-5.3-codex -` via `ProcessBuilder`. Prompt piped via stdin.

## Security: Restricted Sandbox
**CRITICAL:** The `codex` CLI is an agent with shell access, file writes, and web search. AI Pal feeds untrusted web content (HTTP requests/responses from potentially malicious sites) into prompts.

**Mitigation:** Always invoke with:
- `--sandbox read-only` — no file writes
- `--config 'features.shell_tool=false'` — disable shell command execution
- `--config 'web_search="disabled"'` — disable web search
- `--ephemeral` — don't persist session files

Additionally:
- Never pass `--yolo` or `--dangerously-bypass-approvals-and-sandbox`
- Pipe prompt via stdin (`-` argument) to avoid shell injection
- Sanitize/escape prompt content before passing to process

## Files to Modify

### 1. `config/LLMProvider.java`
- Add `CODEX("OpenAI Codex", "localhost")` enum constant

### 2. `config/LLMSettings.java`
- Add model list for CODEX:
  - `gpt-5.3-codex` (default, flagship)
  - `gpt-5.2-codex`
  - `gpt-5.2`

### 3. `config/SettingsManager.java`
- Add persistence keys: `llm.codex.path`, `llm.codex.model`
- Add getters/setters: `getCodexPath()`, `setCodexPath()`
- Default path: auto-detect via common locations (`/usr/local/bin/codex`, homebrew, npm global)

### 4. `llm/impl/CodexClient.java` (NEW)
- Implement `LLMClient` interface
- `complete()` / `chat()`: invoke `codex exec` via ProcessBuilder, parse JSONL response
- `chatStreaming()`: read stdout line-by-line, parse JSONL events, emit `item.type == "agent_message"` text chunks via StreamCallback
- `supportsStreaming()`: return `true`
- `testConnection()`: run `codex --version`, check exit code
- `getProviderName()`: return "OpenAI Codex"
- Build conversation context by concatenating message history into single prompt
- Handle process timeout (default 120s)
- Respect `StreamCallback.isCancelled()` by destroying the process

### 5. `llm/LLMClientFactory.java`
- Add `case CODEX ->` in `createClient()` switch
- Add config validation in `hasValidConfig()` — check codex binary exists

### 6. `ui/settings/LLMSettingsPanel.java`
- Add Codex settings panel section (radio button, path field, model dropdown, test button)
- Auto-detect CLI path on panel load
- Show version on connection test
- "Detect Path" button to find `codex` binary

## CLI Invocation Details

**Non-streaming (pipe via stdin):**
```bash
echo "prompt" | codex exec --json --ephemeral --full-auto --sandbox read-only --config 'features.shell_tool=false' --config 'web_search="disabled"' -m gpt-5.3-codex -
```

**Streaming (same command — JSONL streams line-by-line):**
```bash
echo "prompt" | codex exec --json --ephemeral --full-auto --sandbox read-only --config 'features.shell_tool=false' --config 'web_search="disabled"' -m gpt-5.3-codex -
```

**Connection test:**
```bash
codex --version
```

**Process management:**
- Use `ProcessBuilder` with separate stderr
- Set working directory to temp or user home
- Pipe prompt to stdin, close stream
- Read JSONL from stdout
- Destroy process on cancel/timeout

## JSONL Response Parsing

Extract text from `item.completed` events where `item.type == "agent_message"`:
```json
{"type":"item.completed","item":{"id":"item_3","type":"agent_message","text":"The response text here."}}
```

For streaming, also handle partial events:
```json
{"type":"item.started","item":{"id":"item_3","type":"agent_message","text":""}}
{"type":"item.updated","item":{"id":"item_3","type":"agent_message","text":"partial text..."}}
{"type":"item.completed","item":{"id":"item_3","type":"agent_message","text":"full text"}}
```

Token usage from turn events:
```json
{"type":"turn.completed","usage":{"input_tokens":1234,"output_tokens":567}}
```

## Authentication
Users authenticate via one of:
1. `codex login` — browser OAuth with ChatGPT account (uses subscription)
2. `CODEX_API_KEY` env var — API key for CI/programmatic use (pay-per-token)

AI Pal assumes the user has already authenticated. No credential storage needed in the extension.

## Conversation History
`codex exec` is single-turn. To support multi-turn chat:
- Concatenate conversation history into a single prompt with role markers
- Format: `User: ...\nAssistant: ...\nUser: ...`
- Pass as single prompt via stdin

## Settings UI Layout
```
○ Use OpenAI Codex
  - Run prompts via your local Codex CLI installation

  Path:  [/usr/local/bin/codex    ] [Detect]
  Model: [gpt-5.3-codex ▾]
  [Test Connection]
  Status: Connected - Codex CLI v1.x.x
```

## Verification
1. `./gradlew jar` — builds without errors
2. Load extension in Burp Suite
3. Settings tab: select Codex provider, detect path, test connection
4. Chat tab: send a message, verify streaming response
5. Context menu: right-click request → Analyze for Vulnerabilities
6. Test cancel mid-stream (stop button)
7. Test with invalid/missing `codex` binary — should show clear error

## Open Questions
- Should we support `codex exec --output-schema` for structured vulnerability reports?
- Max timeout value — configurable in settings or hardcoded?
