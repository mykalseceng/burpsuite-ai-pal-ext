# AI Pal v1.1.0

## Highlights

- Added local **Claude Code CLI** provider support via local `claude` CLI integration.
- Added local **OpenAI Codex CLI** provider support via local `codex` CLI integration.
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
  --tools "" \
  --allowedTools ""
```

- `--tools ""` removes all tools from the agent to prevent prompt-injected tool execution.
- `--allowedTools ""` ensures no tools are auto-approved as a second layer of defense.

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

- These are local CLI integrations; users are responsible for complying with Anthropic/OpenAI applicable terms and policies for their account and usage.
- **Detect** searches common install paths and PATH (`which`/`where`) for `claude`/`codex`.
- **Test Connection** validates binary path and executes:
  - `claude --version`
  - `codex --version`
