# Plan: CLI-Based LLM Provider Integrations

## Claude Code CLI Integration
- Provider: `CLAUDE_CODE` — invokes `claude -p` via ProcessBuilder
- Models: `claude-sonnet-4-6` (default), `claude-opus-4-6`, `claude-haiku-4-5-20251001`
- Security: `--allowedTools ""` disables all tools
- Streaming: `--output-format stream-json`
- Auth: User's existing Claude Code subscription (OAuth login)

## OpenAI Codex CLI Integration
- Provider: `CODEX` — invokes `codex exec` via ProcessBuilder
- Models: `gpt-5.3-codex` (default), `gpt-5.2-codex`, `gpt-5.2`
- Security: `--sandbox read-only` + disable shell_tool + disable web_search
- Streaming: `--json` (JSONL format)
- Auth: User's ChatGPT subscription or CODEX_API_KEY

## Shared Design
- Both pipe prompts via stdin to avoid shell injection
- Both sanitize/escape prompt content
- Both support streaming via StreamCallback
- Both auto-detect CLI binary path
- Neither stores credentials — relies on user's existing CLI authentication
