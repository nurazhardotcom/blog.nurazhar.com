Title: Scrum for a Solo Open-Source Project — Building Butler
Date: 2026-06-22
Tags: scrum, agile, project-management, methodology, butler, clojure, ai-agents, lithan
Description: Applying the 2020 Scrum Guide to a solo open-source Clojure project. Roles, events, artifacts, and the Product Backlog for the Butler — from someone learning this in class and building it in real time.

---

I'm taking the **Agile Project Management** module (PDWD-APM-0226) at Lithan Academy as part of my SCTP Professional Diploma in Full Stack Web Development. This post is the practical half of what I'm learning — applying Scrum to a real project I'm building alongside class.

The project is [**Butler**](https://github.com/nurazhardotcom/butler) — a personal AI butler written in Clojure/Babashka. Think Hermes Agent (the #1 app on OpenRouter by token volume) but rebuilt from scratch in Clojure so I control the endpoints, the data, and the architecture. No vendor gatekeeping what models I can use.

This post walks through: Scrum accountabilities in a solo context, the Product Backlog, how Sprint events work when you're a team of one, and the license choice for an open-source agent project.

---

## The Project

Butler is a full rewrite of Hermes Agent capabilities in **Clojure/Babashka** — MIT licensed at [github.com/nurazhardotcom/butler](https://github.com/nurazhardotcom/butler). It started as `financial-butler` (a rule engine for "can I afford this?"), but the Sprint 1 review made it clear: incremental improvement wasn't going to close the gap. The rewrite went live on 22 Jun 2026 with all core subsystems in place.

The architecture is organized around Hermes Agent's key differentiators — the self-improving execution loop, persistent memory, and multi-provider routing:

| Module | What It Does | Status |
|--------|-------------|--------|
| `router.clj` | Multi-provider LLM router with automatic failover (Naraya, OpenRouter, Ollama) | ✅ Done |
| `tools.clj` | Auto-registering tool registry with 10+ built-in tools | ✅ Done |
| `memory.clj` | SQLite FTS5 full-text search across sessions and facts | In progress |
| `context.clj` | Three-tier prompt assembly (stable → context → volatile) | In progress |
| `agent.clj` | Core conversation loop with tool dispatch | In progress |
| `skills.clj` | Skill loading, execution, and auto-generation from traces | Planned |
| `evolution.clj` | DSPy/GEPA-style trace-based self-evolution | Planned |
| `scheduler.clj` | Cron-like recurring task automation | Planned |
| `gateways.clj` | CLI, REPL, and messaging platform adapters | Planned |

The principle: **one agent core, any LLM backend, no vendor lock.** Butler can route through Naraya, OpenRouter, Ollama, or any OpenAI-compatible API. If one provider goes down or kills a feature (as Hermes did with custom endpoints), the router fails over automatically.

---

## Why Scrum for a Solo Project?

The 2020 Scrum Guide (Schwaber & Sutherland) says:

> Scrum is a lightweight framework that helps people, teams and organizations generate value through adaptive solutions for complex problems.

Scrum was designed for teams, but the core mechanisms — **Transparency, Inspection, Adaptation** — apply regardless of team size. A solo developer still needs:

- **Transparency**: The Product Backlog must be visible and the Increment inspectable. If only I know what I'm building, I can't evaluate whether I'm building the right thing.
- **Inspection**: Regular checkpoints to detect problems early. Without a team to catch drift, I need the Sprint cadence to force self-inspection.
- **Adaptation**: Permission to change direction when new information arrives. Solo developers are *especially* vulnerable to sunk-cost thinking — no one is there to say "this isn't working, pivot."

The Sprint is the container that makes all three happen. The Scrum Guide:

> Scrum employs an iterative, incremental approach to optimize predictability and to control risk.

For a project like Butler — building an AI agent with unknown unknowns (LLM behavior, tool-calling edge cases, memory system design) — iterative delivery is the only sensible approach.

---

## Accountabilities (Solo Edition)

The Scrum Guide defines three accountabilities:

| Role | Responsibility | Who Plays It |
|------|---------------|-------------|
| **Product Owner** | Maximizes value of the product. Owns the Product Backlog, the Product Goal, and ordering of work. | **Me** — I decide what features matter most, what "done" means for each release, and what gets cut. |
| **Developers** | Creates the Increment each Sprint. Owns the Sprint Backlog, Definition of Done, and daily adaptation. | **Me** — I write the Clojure code, set quality standards, and adjust the plan. |
| **Scrum Master** | Ensures Scrum is understood and enacted. Removes impediments. | **Me** — I enforce the discipline of the framework, step back to assess process, and unblock myself. |

Three roles, one person. The key insight from the course: **these aren't hats you switch — they're accountability boundaries.** When I'm the Product Owner, I only decide *what* to build, not *how*. When I'm a Developer, I only build and inspect, not change scope. The Scrum Master watches the clock and asks "are we actually doing Scrum right now?"

This separation is harder solo than in a team. The Sprint boundary helps enforce it.

---

## The Product Backlog

The Product Goal: **A personal AI butler that manages finances, coaches life decisions, schedules automations, and improves through use — all under my control, running on any LLM backend I choose.**

The Product Backlog (ordered by value, Sprint 1 increment):

| Priority | Item | Notes |
|----------|------|-------|
| P0 | Multi-provider LLM router with auto-failover | ✅ Done |
| P0 | Auto-registering tool registry (10+ tools) | ✅ Done |
| P0 | Financial rules: `can-afford?`, runway, burn rate | ✅ Done |
| P0 | MCP server — expose tools to any agent | ✅ Done |
| P0 | Namespace rename and repo migration | ✅ Done |
| P0 | MIT license | ✅ Done |
| P1 | SQLite FTS5 persistent memory — cross-session recall | In progress |
| P1 | Three-tier prompt assembly with profile context | In progress |
| P1 | Core agent conversation loop | In progress |
| P2 | Skill generation — "do, learn, improve" loop | |
| P2 | Cron scheduler — recurring tasks and automations | |
| P3 | Self-evolution engine — DSPy/GEPA trace optimization | |
| P3 | Telegram gateway — interact via chat | |

The backlog is transparent by being on GitHub. Classmates and faculty can see what's planned, what's in progress, and what's done.

---

## The Sprint

I'm running **1-week sprints** synchronized with the APM course timeline (SOC 19-Jun, final submission 30-Jun).

### Sprint Planning

At sprint start, I select items from the Product Backlog and define a **Sprint Goal**. For Sprint 1 (22 Jun – 29 Jun):

> Sprint Goal: Hermes-class core — multi-provider router, tool registry, memory system, agent loop. First production-ready Increment.

The Sprint Backlog:
- Namespace rename and repo migration (`financial-butler` → `butler`)
- Multi-provider LLM router with automatic failover
- Auto-registering tool registry with 10+ built-in tools
- SQLite FTS5 persistent memory system
- Three-tier prompt assembly with profile context injection
- Core agent conversation loop with tool dispatch
- MCP server for external agent integration
- MIT license
- README documentation and architecture overview

### Daily Scrum (Self)

The Scrum Guide:

> The Daily Scrum's purpose is to inspect progress toward the Sprint Goal and adapt the Sprint Backlog as necessary.

Solo version, 5 minutes:

1. What did I finish since yesterday?
2. What am I working on today?
3. Is anything blocking the Sprint Goal?

This replaces the "morning coffee + check GitHub" loop with a deliberate question. The key is **adaptation**: if I realize today that the MCP server needs a tool I didn't plan, I adjust the Sprint Backlog.

### Sprint Review

At sprint end, I demo the Increment — in this case, publishing the blog post and deploying the demo. The Sprint Review answers: did we deliver value? Is the Product Backlog still correct based on what we learned?

For Butler, the first Sprint Review revealed:
- The incremental approach wasn't going to bridge the gap to Hermes-level capabilities. Full rewrite was the right call.
- Multi-provider routing with automatic failover is essential — Hermes killing custom endpoints proved this.
- Clojure's multimethods map naturally to Hermes's auto-registering tool pattern.
- SQLite via Python bridge works but needs a more mature persistence story for production use.
- The MCP protocol is elegant — keep this as the primary interop surface.

### Sprint Retrospective

> The Sprint Retrospective's purpose is to plan ways to increase quality and effectiveness.

Solo retrospective, three questions:

1. **What's working?** — Babashka makes CLI tools trivial. Clojure's data orientation makes prompt construction and tool dispatch natural.
2. **What's not working?** — I'm context-switching between APM course, blog writing, and coding. Need bounded sessions.
3. **What will I change?** — Dedicated coding blocks 4-7am (peak productivity). No context switching within a Sprint.

---

## Definition of Done

For Butler, "Done" means:

- [ ] Code compiles (`bb task` / `clojure -M -e ...`)
- [ ] CLI command works with sample input
- [ ] REPL command works for interactive use
- [ ] Error cases handled (missing config, API timeout, invalid input)
- [ ] At least one test (even a REPL smoke test)
- [ ] README updated if behavior changed
- [ ] Blog post published for major features

This is deliberately lightweight. The Scrum Guide says the Definition of Done is team-defined and exists to **instill quality**. For a solo project, the DoD is what prevents "it works on my machine" from being the standard.

---

## License Choice: MIT

Butler is licensed under **MIT** — same as Hermes Agent. Why:

1. **Reciprocity**: Hermes is MIT. A derivative that also targets open-source agent infrastructure should match.
2. **Adoption**: MIT is the most permissive license. Classmates can fork it, companies can use it, no legal friction.
3. **Business model**: I'm not selling software licenses. My business sells services and custom deployments. MIT maximizes distribution.
4. **No vendor lock**: The entire point of this project is that users control their own agent. A restrictive license would contradict the architecture.

The AGENTS.md in the repo documents this explicitly — the license choice is a design decision, not an afterthought.

---

## What This Means for Classmates

We're learning Scrum in class. Most examples are about team projects — 5-9 people, a Product Owner, a Scrum Master, daily standups. That's valuable but it can feel abstract if you're a solo developer or freelancer.

A **sole developer working on open-source** can use Scrum without modifying the framework. The accountabilities still exist (they're just collapsed into one person). The events still happen (they're just faster). The artifacts are still transparent (they're on GitHub).

The critical difference is **external accountability**. In a team, your peers hold you accountable. Solo, you must substitute that with:
- The Sprint cadence itself (a deadline is a deadline)
- Published artifacts (the backlog on GitHub, the Increment in commits)
- Writing — like this blog post — which forces you to articulate what you built and why

If you're building a personal project during the APM course, try mapping it to Scrum. The Product Backlog is a `README.md` or GitHub Issues. The Sprint Backlog is a checklist. The Sprint Review is a demo post. The Retrospective is a note to your future self.

The Scrum framework isn't cargo cult — it's a feedback loop. Even for one person.

---

*Built with [Babashka](https://babashka.org/) and the 2020 Scrum Guide. Course: PDWD-APM-0226, Lithan Academy. Repo: [github.com/nurazhardotcom/butler](https://github.com/nurazhardotcom/butler)*
