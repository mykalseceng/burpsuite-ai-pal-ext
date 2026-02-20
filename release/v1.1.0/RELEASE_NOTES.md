# AI Pal v1.1.0

## Highlights

- Added local **Claude Code CLI** provider support. Use your existing Claude Code subscription via local `claude` CLI login.
- Added local **OpenAI Codex CLI** provider support. Use your existing OpenAI/Codex subscription via local `codex` CLI login.
- Added improved CLI detection and connection validation for both providers.
- Improved chat input UX with clearer input border visibility.

## Security Hardening for Untrusted Input

AI Pal sends untrusted HTTP traffic to LLM providers, so both CLI integrations are constrained to reduce blast radius.

### Claude Code CLI invocation

AI Pal runs Claude Code in prompt mode and explicitly disables tools:

```bash
claude -p \
  --output-format json|stream-json \
  --model <selected-model> \
  --allowedTools ""
```

- `--allowedTools ""` disables agent tools to prevent prompt-injected tool execution.

### OpenAI Codex CLI invocation

AI Pal runs Codex in ephemeral, read-only, tool-restricted mode:

```bash
codex exec --json --ephemeral --skip-git-repo-check \
  --sandbox read-only \
  -c features.shell_tool=false \
  -c web_search=disabled \
  -m <selected-model> -
```

- `--sandbox read-only` reduces write/exec side effects.
- `features.shell_tool=false` disables shell tool usage.
- `web_search=disabled` disables web access.

## Setup Notes

- **Detect** searches common install paths and PATH (`which`/`where`) for `claude`/`codex`.
- **Test Connection** validates binary path and executes:
  - `claude --version`
  - `codex --version`
