Title: Same Tool, Different Brains — Why LLM Inconsistency Drives Me Nuts
Date: 2026-07-02
Tags: llm, opencode, ai, consistency, agentic-dev
Description: Same prompt, same tool (opencode), same task — but different models give completely different behaviors. Why this happens and why it breaks reproducibility in AI-assisted development.

---

> *"what did you just do? omg, different model act differently... mampos lah macam ni"*

That was me today. I asked opencode "are you connected to my GitHub via gh CLI?" Two different model runs. Same tool. Same prompt. Completely different behavior.

One model ran `gh auth status` and showed me the output. The other answered from its training memory.

Same input. Different output. And that's a problem.

```d2
direction: right

You: "Are you connected\nto my GitHub via gh CLI?"
ModelA: "Model A\n(verifier)"
ModelB: "Model B\n(guesser)"
ActionA: "Runs `gh auth status`\nshows real output"
ActionB: "Answers from\ntraining memory"
UserReaction: "Wait — different\nanswers?!"

You -> ModelA: "same prompt"
You -> ModelB: "same prompt"
ModelA -> ActionA: "verifies"
ModelB -> ActionB: "guesses"
ActionA -> UserReaction: "reliable"
ActionB -> UserReaction: "unreliable"

ModelA: {style.fill: "#d4edda"}
ModelB: {style.fill: "#f8d7da"}
```

## Why Models Behave Differently

It's not a bug. It's the *nature of LLMs*.

| Factor | What It Means |
|--------|--------------|
| **Training data** | Each model saw different examples of similar situations |
| **Alignment** | RLHF tuned them to prioritize different response styles |
| **Architecture** | Parameter count, attention mechanisms, context windows differ |
| **Sampling** | Temperature, top-k, top-p create non-deterministic outputs |
| **System prompt** | Each model interprets the same instruction differently |

These aren't small differences. They produce *fundamentally different behaviors* for the same task.

## Why It Matters

**Reproducibility** — If your AI assistant behaves differently every session, you can't build reliable workflows around it. You don't know if it'll verify or guess.

**Debugging** — When something breaks, was it the tool or the model? You waste cycles figuring out which layer failed.

**Trust** — A model that guesses when it could verify teaches you to second-guess everything it does. That cognitive overhead is the opposite of what AI tools should provide.

**Agentic pipelines** — If you chain multiple LLM calls in a pipeline, non-determinism compounds. Each step introduces variance. The output becomes a random walk, not a deterministic process.

## What Helps

**Pin your model** — Know exactly which model is running. Don't leave it to the provider's routing logic.

**Write explicit instructions** — The AGENTS.md pattern works. Tell the model *when* to verify vs. guess. "Always run the command, never answer from memory."

**Test both paths** — Run the same prompt through different models during development. Know where they diverge.

**Accept the constraint** — LLMs are non-deterministic by design. Build guardrails and validation layers around them, don't expect consistency from the model itself.

---

**LLMs don't have consistent personalities. Give them consistent instructions instead.**
