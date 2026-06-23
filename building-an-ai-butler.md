Title: Building an AI Butler: A Post-Mortem From Inside the Architecture
Date: 2026-06-23
Tags: ai, agents, clojure, babashka, hermes-agent, post-mortem, self-hosting
Description: I shipped a Hermes-class personal AI butler in Clojure/Babashka. Five risks hit me while building. Three had architectural fixes. Two did not. Vector at the end.

# Building an AI Butler: A Post-Mortem From Inside the Architecture

If a senior engineer pulled me aside and told me "you're going to build
your own personal AI agent," I'd ask one question before I'd let them
go: **what's the substrate?**

The wrong answer is "let me figure that out later." Because the
substrate decides everything downstream: startup cost, distribution
shape, library reach, threading model, debugging surface, deployment
shape, and whether sub-agent fan-out is cheap or expensive.

Here's the substrate I picked, the five risks that hit me while
building on it, and the three that are bullet-shaped fixes you can ship
this week.

## The substrate: Clojure on Babashka, single binary, ~2 ms startup

| Property | Value |
|---|---|
| Language | Clojure, ~95% of the language surface |
| Runtime | Babashka 1.4 (GraalVM native image, single binary) |
| Startup | ~2 ms cold, no JVM tuning, no classpath contention |
| Concurrency model | core.async channels + future for sub-agent fan-out |
| Distribution | one `butler` script + `src/butler/*.clj` tree |
| Native libs you get | http-client, fs, httpkit, cheshire, jsoup, sci, nrepl |

The trade-off you're paying: no arbitrary JVM libraries (no JDBC, no
Kafka client, no Quartz). Anything not in the bundle gets shelled out.
For my FTS5 use case, that means a Python bridge
(`scripts/sqlite_bridge.py`) the butler calls via
`clojure.java.shell/sh`. Yes, Python. Believe it.

```d2
# Diagram 62
direction: down

title: "Butler — Hermes-class on Babashka" {
  near: "top-center"
}

gateways: "Gateways (you talk to the butler here)" {
  cli: "CLI ./butler"
  repl: "REPL :1667"
  web: "HTMX GUI"
  tg: "Telegram"
}

agent: "Agent loop" {
  context: "3-tier prompt\n(stable + context + volatile)"
  router: "LLM router\n(priority + failover)"
  tools: "Tool registry\n(spec-validated)"
  memory: "Memory store\n(sqlite bridge)"
  skills: "Skills EDN\n(+ auto-evolve)"

  context -> router
  router -> tools
  tools -> memory: "iterate"
}

external: "External (network)" {
  naraya: "Naraya (priority 1)"
  openrouter: "OpenRouter (priority 2)"
  ollama: "Ollama local"
}

gateways -> agent: "user message"
agent.router -> external: "HTTPS POST"
external -> agent.router: "JSON response"
agent.tools -> agent.skills: "save as EDN"
agent.skills -> agent.context: "load into context"
```

The line that matters more than the others: the dashed boundary around
`external`. Everything inside that boundary is local, single-binary,
audit-friendly, offline-tolerant. Everything outside is a network call
you've delegated.

## The five risks

I'm going to walk through them in order of how much they actually hurt
me while building, not in order of glamour.

### Risk 1: stateless persistence (the unsolved problem)

**The symptom.** You're at the kitchen counter on Tuesday morning.
You're paying 2,000 dollars to fix a water heater. Ask your butler
"can I afford it?" Your butler has no idea what you discussed on
Friday. The discretionary cash calculation that worked Friday morning
is unreachable on Tuesday morning. Every session is a first date.

**Why it bites.** A butler with no memory of yesterday is a chatbot.
You feel the friction immediately: the third day in a row you stop
trusting the answer because the context that produced it is gone. You
cave, fall back to "let me check the spreadsheet," and the butler
becomes unused.

**The architectural fix.** Persist the volatile tier to a single EDN
bundle at session close, load it on session open. Pin yesterday's
summary into today's volatile tier so the LLM sees it from message
one.

```d2
# Diagram 63
direction: down

title: "title:" {
  label: "Stateless persistence — save + load bundle"
  near: "top-center"
}

session_close: "Session close\n(scheduler 23:55)" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
memory_dump: "memory summary\ntop 10 facts" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
traces_dump: "tool traces\nlast 50 calls" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
volatile_dump: "current volatile tier" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
bundle_write: "agent/bundle.edn\nEDN blob" {
  style.fill: "#e9ecef"
  style.stroke: "#ced4da"
}
morning_wake: "Morning wake\n(scheduler 04:00)" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
bundle_read: "Read bundle EDN" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
volatile_pin: "Pin yesterday\ninto volatile tier" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
first_message: "First user message\nof the day" {
  style.fill: "#e2e3e5"
  style.stroke: "#d6d8db"
}

session_close -> memory_dump
session_close -> traces_dump
session_close -> volatile_dump
memory_dump -> bundle_write
traces_dump -> bundle_write
volatile_dump -> bundle_write
morning_wake -> bundle_read: "next day"
bundle_read -> volatile_pin
volatile_pin -> first_message
```

Code shape, not pseudocode — what you'd actually write:

```clojure
(defn save-bundle! [profile session-id]
  (let [bundle {:memory-summary (memory/recall profile "" 10)
                :recent-traces (tools/execution-traces 50)
                :volatile-snapshot (context/last-volatile! session-id)
                :saved-at (java.time.Instant/now)}]
    (->> bundle
         (with-out-str (clojure.pprint/pprint))
         (spit (profiles/data-dir profile "bundles" session-id ".edn")))
    :saved))

(defn load-bundle [profile]
  (let [latest (latest-bundle-file profile)
        bundle (when latest (clojure.edn/read-string (slurp latest)))]
    (when bundle
      (context/pin-to-volatile!
        (:volatile-snapshot bundle)))))
```

The scheduler does the closing side. An init-on-boot hook does the
opening side. Two functions, one EDN blob per profile per night.

**Severity: high.** This is the structural change between "chatbot
with a Clojure accent" and "thing that knows me." Ship this week.

### Risk 2: tool breadth seduction

**The symptom.** You read the Hermes Agent docs. They have 70+
built-in tools across 28 toolsets. You feel the dopamine. You add a
PDF tool. Then a webfetch tool. Then a calendar tool. Then a contacts
tool. After 30 days you have 11 tools and use 4 of them daily. Seven
tools are dead code you still have to maintain.

**Why it bites.** Each tool is Clojure code. Clojure code is specs,
tests, edge cases, calls into Java, error handling for malformed
arguments, result sanitization so prompt injection doesn't land in
your LLM context. Each line of that has maintenance entropy. Entropy
compounds when nobody calls the tool.

**The cure.** Not code. Discipline. When you feel the urge to add a
tool, ask: *"Did yesterday's routine fail without this?"* If no,
defer. Re-check in 30 days. If still no, never add.

For me that's true for seven of the eleven. The four that fire:
`can_afford`, `search_memory`, `shell`, `read_file`. The rest are
demo-grade and will rot unless the daily routine forces them awake.

**Severity: medium.** Doesn't kill the project. Slows it down. The
hard part isn't deleting the tools in your code; it's deleting the
dream of the tools you planned to add tomorrow.

### Risk 3: auto-evolution regression

**The symptom.** Your evolution module analyzes tool traces nightly
and proposes skill improvements. Cool. On night seven it rewrites the
`can_afford` skill. From that morning onward your butler returns the
same answer whether you ask about 500 dollars or 50,000. The numbers
have stopped meaning anything. You didn't notice, because the response
*looks* confident.

**Why it bites.** Your butler is now confidently wrong. Worse than
"I don't know," because you keep trusting it. Eventually you get a
real loss and stop using the feature.

**The architectural fix.** An eval harness. A fixed suite of "known
good" prompts with expected answers, locked in
`tests/known-good.edn`. Every time evolution saves a new version of
any skill, the harness re-runs against the new version. Any
regression → automatic revert to prior version.

```edn
;; tests/known-good.edn
{:skill "can-afford"
 :cases
 [{:input {:amount 500 :balance 30000 :debt 2000 :expenses 4500}
   :expected {:approved true  :headroom 16500}}
  {:input {:amount 99999 :balance 30000 :debt 2000 :expenses 4500}
   :expected {:approved false :shortfall 83499}}]}
```

Code shape:

```clojure
(defn evaluate-skill [profile skill-name]
  (let [tests (clojure.edn/read-string
                (slurp "tests/known-good.edn"))
        skill (skills/load-skill profile skill-name)
        results (for [{:keys [input expected]} (:cases tests)]
                  [input expected (skill input)])]
    (doseq [[in exp actual] results]
      (assert (= exp actual)
        (str "Skill regression in " skill-name
             "\n  input:    " (pr-str in)
             "\n  expected: " (pr-str exp)
             "\n  actual:   " (pr-str actual))))
    :ok))

(defn evolve-skill-guard [profile skill-name]
  (let [prior-version (skills/load-skill profile skill-name)]
    (try
      (let [new-skill (evolution/evolve-skill! profile skill-name)]
        (evaluate-skill profile skill-name))
      (catch Exception e
        (println "Regression - reverting" skill-name)
        (skills/save-skill! profile prior-version)))))
```

**Severity: medium today, high at scale.** The longer auto-evolution
runs unattended, the more it accumulates quiet regressions. Run the
guard before you trust one-night iteration.

### Risk 4: SaaS agent platform crowd-out (theoretical)

**The symptom.** You read news about OpenAI's new assistant,
Anthropic's new tools, or a startup with a hundred-million-dollar
seed round. "What if they ship a butler next year? I wasted my time."

**Why it doesn't bite.** They sell to millions. You're one user. They
can't ship your rules, your finance policy, your audit trail, your
SQLite FTS5 queries, your Clojure composability, your disk-resident
memory, your prompt-injection-bounded MCP server. Different problem
space.

**The cure.** Recognition, not code. You're not building a competitor
to OpenAI's product. You're building an operating system for your own
financial and procedural decisions. That's not a market. That's you.
The threat is theoretical unless you decide to exit self-hosting
because it's easier to rent someone else's butler than run your own.

**Severity: low.** Real only at the moment you decide to ship your
butler as a product. At single-user scale, it's about as relevant as
"the tax office will ship a free accounting tool someday."

### Risk 5: scope creep (the killer)

**The symptom.** Today you ship finance rules because they're the
most rule-bounded decision in your life and they're valuable as
audit. Tomorrow you add calendar. Then contacts. Then email triage.
Then a blog draft assistant. Then recipe management. Six months in
your butler does 30 percent on each domain and zero percent
excellently. The flagship (finance) loses maintenance attention to
game-playing toys.

**Why it kills.** Splits your maintenance attention into N
half-finished artifacts. Compounds when you start avoiding the
maintenance because the half-finishedness is depressing. Slowest
burn of the five. Maximizes regret over the longest timescale.

**The cure.** Discipline, not code. Pick the binding constraint.
Ship it. Maintain it for 90 days. Then pick the next. The vector
toward "more butler" is small, each step deliberate, each shippable.
Do not start a new vertical while an existing one is broken.

For me, the binding constraint was finance. That was right. Calendar
is not the binding constraint right now. So I am not building a
calendar tool until my daily routine fails without it for at least
30 days.

**Severity: highest.** Of all five. Because the others have
architectural fixes; this one doesn't.

## What to ship this week

If you're pairing with me and we have one calendar week to ship one
thing, here is what we ship, in this order:

1. `agent/save-bundle!` and `agent/load-bundle`. Two functions. One
   EDN blob per profile per day. ~40 lines of Clojure. Solves
   Risk 1.
2. Wire save to scheduling. A 23:55 cron entry that calls save-bundle.
   ~5 lines.
3. Wire load to morning wake. Init hook in core/init! reads the most
   recent bundle, pins yesterday's volatile snapshot to today's
   volatile tier. ~10 lines.

That's the week. After Risk 1 is solved, the eval harness (Risk 3)
has ground truth. With eval harness solid, you can let auto-evolution
run unattended for 30-day stretches, which makes scope creep tractable
because you now have evidence about which skills improve and which
rot.

Order matters. Do not start Risk 3 before Risk 1.

## What I'd tell past-me

If I were starting this six months ago, I'd still build it. I'd
pick Babashka for the same reason. I'd build the same five layers
in the same order. I'd ship Risk 1 on day one, not day 30. I would
not invent engineering work for Risk 2 or Risk 5, because tool
breadth and scope creep are discipline problems and pretending
they're engineering problems just slows you down. I'd run the eval
harness from Risk 3 starting day 14, not day 90, because the first
time you save a corrupted skill and trust it for 11 days is when
you stop trusting the system.

What I'd *not* do: blame the substrate. The substrate is fine.
Babashka is fine. Clojure is fine. SQLite FTS5 is fine. The
constraint graph is in your head, not in the runtime.
