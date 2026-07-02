Title: Connecting OpenCode to Hermes Agent — No GUI Required
Date: 2026-07-02
Tags: opencode, hermes, hermes-agent, cli, llm, ai-agents, architecture, devops, self-hosting
Description: OpenCode and Hermes Agent both want to be your AI interface — but they serve different roles in the stack. Here's how to wire them together so they complement instead of compete.
---

```d2
direction: right

OpenCode: "OpenCode\nTerminal / Desktop"
HermesAPI: "Hermes Agent\nHTTP Server :9119"
HermesCore: "Hermes Core\nRouting + Memory"
Models: "Any LLM\nCohere / OpenAI / etc"
Tools: "Hermes Tool\nRegistry + Exec"

OpenCode -> HermesAPI: "OpenAI-compatible\nPOST /v1/chat/completions"
HermesAPI -> HermesCore: "Internal dispatch"
HermesCore -> Models: "Provider router"
HermesCore -> Tools: "Tool execution"
HermesCore -> OpenCode: "Response + memory"
```

I use both daily — OpenCode for the terminal interface, Hermes Agent for the memory and provider routing. They don't compete. They're two layers of the same stack. But connecting them took figuring out why the GUI captures the port.

**OpenCode** is the UI layer (terminal/desktop). **Hermes Agent** is the backend (API server, provider router, persistent memory, tool registry). Together they form a local-first AI pipeline where nothing hits the cloud unless you route it that way.

## The `--no-gui` Trick

`hermes serve` launches the full Hermes Desktop GUI, which binds port 9119 and won't release it. OpenCode can't connect. The fix:

```bash
hermes serve --no-gui &
```

This starts the **HTTP server only** — no Electron window, no GUI rendering, just the OpenAI-compatible endpoint on port 9119. OpenCode talks to it like any OpenAI API.

| Mode | Port 9119 | OpenCode connects? | What you get |
|------|-----------|-------------------|--------------|
| `hermes serve` | GUI captures it | No | Desktop app |
| `hermes serve --no-gui` | HTTP server | Yes | API-only backend |

## Wiring OpenCode

Point OpenCode at the Hermes endpoint. Config file at `~/.config/opencode/opencode.json`:

```json
{
  "api": {
    "endpoint": "http://localhost:9119/v1",
    "model": "cohere/command-a-plus-05-2026",
    "api_key": "",
    "temperature": 0.7,
    "max_tokens": 4096
  }
}
```

OpenCode sends OpenAI-format requests. Hermes translates, routes to the chosen provider, and returns the response with memory context attached.

## What Hermes Adds

| Feature | Without Hermes | With Hermes |
|---------|---------------|-------------|
| **Model switching** | Change config manually | Router handles it |
| **Memory** | Ephemeral | Persisted to SQLite |
| **Tool execution** | OpenCode native only | Hermes registry + OpenCode |
| **Provider abstraction** | None | Any OpenAI-compatible |

## The Network Flow

```d2
direction: down

OpenCode: "OpenCode CLI"
Hermes: "Hermes Agent\n:9119"
Router: "Provider Router"
LLM: "Cohere command-\na-plus-05-2026"

OpenCode -> Hermes: "POST /v1/chat/completions"
Hermes -> Router: "Parse + select provider"
Router -> LLM: "Forward request"
LLM -> Router: "Stream tokens"
Router -> Hermes: "Attach memory context"
Hermes -> OpenCode: "JSON response"
```

## Security

Everything runs on localhost (`127.0.0.1`). No data leaves your machine unless you configure Hermes to route through a cloud provider. The memory persists in an encrypted SQLite database at `~/.local/share/hermes/memory.db`.

## The Takeaway

`hermes serve --no-gui` is the one-liner you need. OpenCode for the terminal. Hermes for the backend. Both on localhost, zero cloud dependency, one port.

*Published from OpenCode terminal — inference via Hermes Agent routing to Cohere command-a-plus-05-2026.*
