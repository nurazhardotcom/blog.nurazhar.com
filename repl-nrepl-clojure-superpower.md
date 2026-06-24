Title: REPL vs nREPL — Why Clojure's Live Programming Is a Superpower
Date: 2026-06-24
Tags: clojure, babashka, repl, nrepl, lisp, workflow, philosophy
Description: REPL is the eval loop. nREPL is the wire protocol that connects your editor to a running Clojure process. Together they give Clojure a feedback loop measured in milliseconds, not seconds. Here's why that matters.

# REPL vs nREPL — Why Clojure's Live Programming Is a Superpower

If you've never worked in a Lisp, the first time you connect a REPL to a
running process and inject a function definition *without restarting* feels
like a cheat code.

I've been building Butler — a personal AI agent in Clojure/Babashka —
and every session starts the same way: `bb task nrepl`, connect from my
editor, and I'm inside a live system. I edit a function, send it to the
REPL, and test it immediately. No compile step. No redeploy. No restart.

This is normal in Clojure. It is almost unheard of everywhere else.

## What Is a REPL?

REPL stands for **Read-Eval-Print Loop**. It's the interactive prompt you
get when you run `clojure`, `bb`, `python3`, or `node` with no arguments.
You type an expression, it reads it, evaluates it, prints the result, and
loops.

```
bb -e '(+ 1 2 3)'
;; => 6
```

That's a REPL — but it's the most basic form. A bare REPL on a terminal
is like using a calculator: fine for one-liners, useless for building
systems.

## What Is nREPL?

**nREPL** is a *network REPL*. It's a protocol — not a tool — that lets
a client (your editor) talk to a running Clojure process over TCP.

| | REPL | nREPL |
|---|---|---|
| **Transport** | stdin/stdout | TCP socket |
| **Client** | Terminal | Editor (Emacs, VS Code, IntelliJ) |
| **Session** | One-shot | Persistent, long-lived |
| **State** | Per-invocation | Shared across connections |
| **Use case** | Quick eval | Live system development |

When I run `bb task nrepl` in Butler, it starts an nREPL server on port
1667. My editor connects to it. Every function I send goes into the *same
running process*. The atoms, the state, the loaded namespaces — all of it
is live and mutable from my editor.

```clojure
;; I edit a function in my editor, then send this to the REPL:
(butler.dev/test-tool "can_afford" {:amount 500})
;; => {:content "✓ Approved — $500 fits.", :headroom 29600}
```

The system was already running. I just injected new behavior and tested it
in under a second.

## Why This Is a Superpower

### 1. The Feedback Loop Is Milliseconds

This is the big one. In most languages, changing code means:

1. Edit → 2. Save → 3. Wait for compiler → 4. Wait for build → 5. Restart
   process → 6. Navigate to test → 7. Run → 8. Observe result

In Clojure with nREPL:

1. Edit → 2. Send to REPL → 3. Observe result

Steps 3-8 of the traditional flow are eliminated. Not accelerated.
*Eliminated.* The process never stops. The state never resets. You don't
reconnect to the database. You don't re-authenticate. You just keep
working.

### 2. You Can Fix Production Without Restarting

This sounds terrifying until you've done it. A running service has a bug
in a hot path. You connect nREPL to the production process, find the
broken function, redefine it, and the next request hits the fixed code.

No deployment pipeline. No container rebuild. No traffic drain. The fix
is live before your CI pipeline would have even started.

This is not theoretical. Clojure shops do this. The nREPL connection is
secured (SSH tunnel, auth), and the operation is atomic — `swap!` on a
single function var.

### 3. You Can Grow a Program From the REPL

In the Butler project, I started with a single `(println "hello")` in a
file. From that minimal entry point, I connected nREPL, then built the
entire system function by function — each one tested and verified before
moving to the next.

This is called *REPL-driven development* and it inverts the traditional
workflow:

- **Traditional**: Write all the code, then run it, then debug it.
- **REPL-driven**: Run a minimal process, then grow the code into it.

The program is never "not running." You start with a live process and
add layers on top of it, testing each layer immediately.

### 4. State Is Explicit and Observable

Because the process stays alive, you can inspect state at any time:

```clojure
;; What tools are registered?
(require '[butler.tools :as tools])
@tools/registry
;; => {"echo" {...}, "shell" {...}, "can_afford" {...}, ...}

;; How many providers are configured?
(require '[butler.router :as router])
(count @router/providers)
;; => 3
```

The atoms are right there. You can `deref` them, watch them,
`add-watch` them. There is no hidden state.

## nREPL Is Not the Only Option

Babashka supports nREPL natively (that's what `bb task nrepl` uses).
There's also:

- **prepl** — A simpler, JSON-based REPL protocol (Babashka supports
  this too, via `bb --prepl`)
- **socket REPL** — A raw TCP REPL built into Clojure JVM
- **REPL.it / Clerk** — Browser-based REPLs for notebooks

But nREPL is the de facto standard for editor integration because of its
support for middleware: you can add custom ops for completion,
lookup, linting, and more.

## Why Other Languages Don't Have This

Most languages compile to a binary or bytecode that runs in a separate
process. You can't swap a function in a running C program. You can't
redefine a class in a running Java program without hotswap agents that
are brittle and limited.

Clojure runs on hosted platforms (JVM, JavaScript, Babashka's SCI) that
support dynamic evaluation. Combined with Clojure's **homoiconicity**
(code is data), you can trivially construct and evaluate arbitrary
expressions at runtime.

The combination is unique: a hosted, dynamic, functional language with a
standardized network protocol for live programming.

## The One-Liner Test

If I need to explain why I use Clojure to another engineer, I show them
this:

```clojure
;; Terminal 1: Start a process that stays running
bb -e '(spit ".nrepl-port" "1667") (require (quote babashka.nrepl.server)) ((requiring-resolve (quote babashka.nrepl.server/start-server!)) {:port 1667}) (while true (Thread/sleep 10000))'

;; Terminal 2 or editor: Connect and redefine anything
```

The process stays alive. I redefine behavior. The next call reflects the
change. No other ecosystem makes this this easy.

---

*Built with Babashka and nREPL, both running on a single binary with
~2ms startup. The nREPL server for Butler runs on port 1667.*
