Title: Hermes Terminated Custom Endpoint API — Why This Matters
Date: 2026-06-22
Tags: hermes, api, gatekeeping, ai-infrastructure, vendor-lock-in
Description: Hermes removed the bring-your-own-endpoint feature for LLM routing. Here is why it happened, the pattern it follows, and what to do about it.

---

I found out the hard way. My Hermes agent had a custom endpoint configured pointing to OpenModel — a multi-model LLM gateway. It worked fine. Then one day, the GUI showed it was terminated. No warning. No explanation. Just gone.

I reached out. Hermes confirmed: they removed the "bring your own endpoint" feature. Custom API endpoints are no longer supported.

## What Hermes Did

Hermes (Naraya) is a router that aggregates multiple LLM providers under a single API. You get one key, one endpoint, and pick a model. Originally, they also let you plug in your own API endpoint — any OpenAI-compatible provider. You could route through Hermes to whatever model you wanted.

That feature is dead. The UI shows it. The API rejects it. Custom endpoints have been removed.

## What's Left

The managed models still work. You route through `router.naraya.ai` with models like:

| Model | Context | Reasoning | Vision |
|---|---|---|---|
| deepseek-v4-flash-naraya | 1M | yes | no |
| claude-sonnet-4.5 | 200K | no | no |
| claude-haiku-4.5 | 200K | no | yes |
| qwen3.7-max-naraya | 1M | yes | no |
| mistral-large | 252K | no | no |
| minimax-m3 | 1M | yes | yes |
| deepseek-3.2 | 131K | no | no |

These still work because they run on Hermes' infrastructure. The key difference: **you pay Hermes, not your provider directly.** What they buy wholesale from Anthropic/DeepSeek gets marked up and sold to you.

## Why This Pattern Repeats

This is not unique to Hermes. Every LLM gateway does this eventually:

1. **Phase 1:** Launch with custom endpoints — "bring your own key, we just route."
2. **Phase 2:** Build managed model integrations — better margins, better control.
3. **Phase 3:** Deprecate custom endpoints — support burden, no margin, competitive risk.

The pattern is predictable. If you depend on a gateway's custom endpoint feature for production traffic, you need a fallback. The gateway will eventually remove it.

## What I Did Instead

I wrote a 100-line Babashka proxy that talks directly to the APIs I need. It does exactly what Hermes' custom endpoint did: accept an OpenAI-compatible request, forward it to my chosen provider, return the response. No markup. No feature removal risk.

The Financial Butler (my Clojure-based personal finance agent) now uses this directly via the Naraya router's managed models where it makes sense, and direct API calls where it doesn't.

## The Lesson

> Any platform feature you depend on that isn't your product will eventually be removed.

Hermes' product is their managed model lineup. Custom endpoints were a feature they offered to bootstrap adoption. Once they had enough users on managed models, the feature became a liability.

If you're building on an LLM gateway:
- Treat custom endpoints as a temporary bridge, not infrastructure
- Keep a direct provider key as backup
- Know how to switch in hours, not days

I didn't lose anything critical — the Financial Butler works fine on `deepseek-v4-flash-naraya` through the managed router. But I was reminded that when a platform removes a feature, it's not a bug. It's a strategy.
