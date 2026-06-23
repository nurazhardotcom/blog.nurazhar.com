Title: Will LLM Tokens Become Like Bandwidth — Abundant and Cheap?
Date: 2026-06-22
Tags: llm, ai, economics, infrastructure, inference, pricing
Description: Mapping the submarine cable story to LLM inference — competition, open-source, and whether tokens will ever be "unlimited" like bandwidth.

---

Singapore has ~28 submarine cables landing on a small island. Competition between cable owners drives wholesale bandwidth prices down to some of the cheapest in Asia. No single carrier owns your route — Singtel, Google, Meta, China Mobile, Reliance Jio all have capacity. The result: bandwidth is effectively abundant.

The same dynamics are emerging in LLM inference. Here's the mapping, and where it breaks.

---

## The Analogy

| Submarine Cables | LLM Inference |
|-----------------|---------------|
| Fibre-optic cable | GPU cluster running a model |
| Cable capacity (bandwidth) | Compute capacity (FLOPs) |
| Cable consortium | Model provider (OpenAI, Anthropic, Google, Meta) |
| Landing station | Data centre / API endpoint |
| Terrestrial backhaul | Model architecture / context window |
| Price per Mbps | Price per token |
| Unlimited bandwidth | Unlimited tokens |

Both are infrastructure layers where the cost structure is: **high fixed cost to build, near-zero marginal cost to serve** — but only beyond a certain threshold.

---

## Why Bandwidth Became Cheap

Bandwidth got cheap because:

1. **Fibre is abundant** — once the cable is laid, sending one more photon costs essentially nothing. The marginal cost of one more bit is zero.
2. **Competition** — multiple cables to the same destination means carriers compete on price. Singapore → US has Bifrost, Echo, SEA-US — three separate systems.
3. **No quality differentiation** — a bit carried by Bifrost is the same bit carried by Echo. It's a pure commodity.
4. **Supply exploded** — newer cables (Bifrost, Echo, Apricot) each add 200+ Tbps capacity. Demand grows but supply grows faster.
5. **Hyperscalers overbuild** — Google and Meta build their own cables. They don't need to make a profit on the cable itself; it enables their core business.

## Why Tokens Are Getting Cheaper

The same forces are at play:

1. **GPU compute is getting cheaper** — H100 → B200 → next-gen, each generation more FLOPs per dollar. Inference optimization (speculative decoding, quantization, KV-cache tricks) multiplies the effective throughput.
2. **Competition** — OpenAI, Anthropic, Google, Meta, Mistral, DeepSeek, and dozens of open-source providers compete on price. GPT-4o mini, Claude Haiku, Gemini Flash, Llama 3 — all racing to the bottom on per-token cost.
3. **Open-source creates a price floor** — Llama 3.1 405B is free to serve if you have GPUs. The API providers can't price above your self-hosting cost by much. This is the same dynamic as "I could land my own cable at Tuas" keeping Singtel's wholesale rates honest.
4. **Hyperscalers don't need to profit on inference** — Google serves Gemini cheaper than pure-play AI companies because it unlocks Search, Cloud, and Ads revenue. Meta gives Llama away for free because it defends their social graph. Same as Google and Meta building cables — it's an enabler, not a profit centre.
5. **Supply is exploding** — new GPU clusters come online monthly. The compute supply curve is steepening.

### Current Pricing Trajectory

Since GPT-3 (2020) to today (2026):

| Model | Tokens per $1 | Change |
|-------|--------------|--------|
| GPT-3 (2020) | ~700 | — |
| GPT-3.5 (2022) | ~20,000 | ~30x cheaper |
| GPT-4o mini (2024) | ~15,000,000 | ~20,000x cheaper |
| Frontier models (2026) | ~5,000,000+ | Continuing trend |

In six years, token cost dropped ~20,000x. That's faster than Moore's Law ever was.

---

## Where the Analogy Breaks

### Commodity vs Quality Differentiation

A bit is a bit. A token from GPT-4 is not the same as a token from Llama 3. Quality, reasoning ability, latency, context window — these differentiate tokens in a way that doesn't exist for bandwidth.

This means **tokens will never be a perfect commodity**. The top-tier reasoning model will always command a premium over a fast small model, just like fresh salmon costs more than canned tuna.

### Marginal Cost Floor

Bandwidth marginal cost is truly zero — the laser is already on, sending more pulses costs nothing. Inference marginal cost is the GPU energy + amortised hardware. For a frontier model on H100s:

- Energy per token: ~0.001 Wh → ~$0.0000001 at industrial rates
- Hardware amortisation per token: ~$0.000001
- Total marginal cost: ~$0.000001 per token (frontier) — not zero, but very small

The floor exists but it's low.

### Latency Constraints

Bandwidth is a rate (Mbps). Tokens are a rate (TPS) with a latency requirement. You can't batch arbitrarily to drive down cost if the user expects a response in <1 second. This prevents the full "fill the pipe" economics of bandwidth.

### Model Training Fixed Cost

Every cable consortium pays ~$300-700M to build the cable, then recoups over 25 years. Every AI company pays $100M-1B to train a frontier model, then recoups over ~1-2 years. The shorter recoup window means pricing is more sensitive to training costs.

Open-source eliminates this entirely (Meta spends on training, you spend on inference only).

---

## The Likely Outcome

**Commodity inference becomes very cheap but not free.** Think:

| Tier | Price per million tokens | Analogy |
|------|-------------------------|---------|
| Fast small model (Llama 3 8B, GPT-4o mini) | ~$0.01-0.05 | Residential broadband |
| Good medium model (Claude Sonnet, Gemini Pro) | ~$0.10-0.50 | Business fibre |
| Frontier reasoning (GPT-5, Claude Opus) | ~$1-5 | Dedicated international circuit |

The gap between tiers will compress over time as distillation and architecture improvements push frontier quality into smaller models.

**Unlimited tokens will exist — with caveats.** Several providers already offer "unlimited" at a monthly subscription (e.g., $20/month for capped access to fast models). This is exactly the residential broadband model — unlimited at 100 Mbps, but Gigabit costs extra.

The true unlimited scenario (any model, any volume, $0 marginal cost) requires:

1. **Model training cost approaches zero** — either via open-source or 10,000x training efficiency gains
2. **GPU energy cost approaches zero** — requires energy abundance (fusion/solar overbuild)
3. **Latency ceases to be a constraint** — agents queue work asynchronously (already happening)
4. **Quality commoditisation** — either all models converge to similar capability, or the premium tier becomes so cheap it doesn't matter

Condition 4 is already happening (model convergence). Condition 3 is happening (agentic workflows). Conditions 1-2 are physics and engineering timelines — likely 5-10 years.

---

## The Cable Map Lesson

The submarine cable map shows 28 cables landing in Singapore because the government made it easy, the hyperscalers needed capacity, and competition drove prices down.

The LLM inference market has the same structural forces:

- **Regulatory openness** (open-source is allowed everywhere, no permission required)
- **Hyperscaler overbuild** (Google/Meta/Microsoft don't need inference to be a profit centre)
- **Competition across layers** (infrastructure, model, application)
- **Supply growing faster than demand** (more GPUs, better architectures, optimised serving)

The cables took 150 years to get to 28 lines on a map. The LLM market did the same in 5 years. Token prices will follow the bandwidth curve — maybe faster.

---

## Bottom Line

Will tokens be "unlimited"? In the same way bandwidth is "unlimited" — yes, for the commodity tier, within reasonable usage, with a monthly cap. For frontier reasoning, you'll pay a premium, but that premium will drop 10-100x over the next 3 years.

The submarine cable analogy holds: **infrastructure competition drives price to marginal cost, and hyperscaler vertical integration accelerates the race to the bottom.** The only limit is the energy cost of flipping the transistors.

---

*Pricing data from public API pages, 2020-2026. Bandwidth economics from TeleGeography and IMDA. The author runs inference locally for some workloads and pays for frontier APIs for others — the optimal split is itself a function of these economics.*
