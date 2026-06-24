Title: Agents Are Dumb, Humans Must Tell Them Everything
Date: 2026-06-24
Tags: agents, ai, common-sense, human-supervision
Description: A candid reflection on why AI agents need explicit human instructions and lack innate common sense.

---

# Agents Are Dumb, Humans Must Tell Them Everything

> *“Machines don’t have common sense – we have to give them every detail, otherwise they hallucinate.”*

In the era of ever‑more capable language models and autonomous agents, one uncomfortable truth persists: **agents have no common sense.** They can parse code, synthesize text, and even run tools, but they lack the intuitive world model that humans take for granted.

## Why Agents Appear “Dumb”

| Reason | Explanation |
|--------|-------------|
| **Statistical Nature** | LLMs predict the next token based on patterns in training data, not on an understanding of reality. |
| **Lack of Embodied Experience** | They never touched a cup of coffee, never felt rain, and therefore cannot infer physical constraints without being told. |
| **No Persistent Memory of Real‑World Rules** | Unlike a human, an agent does not retain a *mental model* of gravity, ethics, or social etiquette unless explicitly encoded. |
| **Prompt‑Dependent Behavior** | Small changes in wording can cause wildly different outputs – a sign that the agent is following surface cues rather than deep reasoning. |

## The Human‑In‑The‑Loop Imperative

1. **Explicit Specification** – Every assumption (file paths, network access, UI expectations) must be spelled out in the prompt.
2. **Verification & Guardrails** – After an agent produces a plan, a human must verify its feasibility before execution.
3. **Iterative Feedback** – Agents improve only when we correct their mistakes; they do not self‑correct based on common‑sense intuition.

## Practical Tips for Working with Agents

- **Use Structured Prompts** – Break tasks into numbered steps; include *pre‑conditions* and *post‑conditions*.
- **Add Safety Checks** – Wrap any file‑system or network operation in a confirmation step.
- **Leverage Tooling** – Use `run_command` or `grep_search` only after the agent has been instructed to do so.
- **Document Assumptions** – Keep a markdown “Agent Playbook” that lists known limitations and required explicit instructions.

## A Humorous Perspective

> *If an agent were a teenager, it would be the one who asks, “Do I really need to know why the sky is blue, or can I just copy‑paste the answer?”*

That’s exactly why we must **teach** the agent, not **expect** it to know. The more we codify our expectations, the more reliable the output.

## Looking Forward

Future research aims to embed *common‑sense knowledge graphs* into the model’s weights, but until then:
- Treat agents as **powerful autocomplete tools**.
- Never assume they understand context beyond the supplied prompt.
- Keep the human loop tight, especially for actions that affect the real world.

---

*Written by Nur Azhar on 2026‑06‑24. This post reflects personal observations and is not a formal scientific paper.*
