Title: How Hermes Agent Memory Actually Works
Date: 2026-06-19
Tags: ai, hermes, memory, tools

# How Hermes Agent Memory Actually Works

Unlike conversational AIs that absorb patterns unconsciously, Hermes uses **explicit durable memory** - you decide what gets stored and what gets forgotten.

## The Memory Architecture

```d2
# Diagram 116
direction: down

a: "~/.hermes/memories/user.md"
b: "Character Budget"
c: "~/.hermes/memories/memory.md"
d: "Hermes Session"
e: "LLM Decision"
f: "Skills Directory"
g: "Explicit memory calls"
h: "Explicit skill calls"

b -> d: "Injected into every prompt"
```

## Key Constraints

- **Hard 1,375 character limit** per memory file
- No unconscious pattern absorption
- No session-to-session learning without explicit `memory` tool calls
- Skills (`skill_manage`) for procedural memory - separate from user facts

## Why This Matters

| Feature | Hermes Agent | Perplexity/Antigravity |
|---------|--------------|------------------------|
| Pattern learning | ❌ Explicit only | ✅ Unconscious absorption |
| Memory limit | 1,375 chars hard cap | Unlimited cloud storage |
| Memory drift | Impossible - you control all writes | Inevitable - patterns shift |
| Tool execution | ✅ Direct to your terminal | ❌ Chat only |

## Practical Implications

When you tell Hermes something via `memory`, it:
1. Gets stored in plain text markdown
2. Is injected into every subsequent prompt in that profile
3. Can be read/modified/deleted anytime via `memory` tool

No magic. No drift. No unconscious learning. Just explicit, durable facts you control.

> This blog post was written to clarify how Hermes memory works after confusion about unconscious pattern absorption. It's a feature, not a bug - you stay in control.