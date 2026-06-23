Title: The Browser is Fragile: Why I Nuked My Web GUI for Native Clojure Desktop
Date: 2026-06-08
Tags: clojure, cljfx, desktop-gui, multi-agent-system, career-ops, automation
Description: I tried building headhunter-agent as a web app. It broke. In minutes, I nuked the browser code and rewrote it as a native cljfx desktop app. Here is why.

---

I've been building **`headhunter-agent`**—a local-first job search assistant and resume compiler—to automate my career strategy. 

Initially, I tried building it as a web app. It was a fragile mess. In under ten minutes of debugging browser errors, I nuked the web files and rewrote the entire thing as a native Clojure desktop app using **`cljfx` (JavaFX)**. 

Here is the unfiltered story of how it went down, and why the browser is the wrong sandbox for local-first tools.

---

## 1. The Design: Deep Profiling & Multi-Agent Evaluations

The project started as a fork of `career-ops`, a basic Babashka CLI. But standard resume builders are shallow keyword-stuffers. They produce generic garbage that falls apart in real interviews. 

To fix this, I re-architected it around three core requirements:
- **A Data Vault:** An EDN database containing full qualifications, deep work history, and a curated library of **8-12 STAR stories** (Situation, Task, Action, Result).
- **A 3-Stage Multi-Agent System (MAS):**
  - **Agent 1 (FCF Audit):** Auditing the Job Description for legitimacy and Singapore's Fair Consideration Framework red flags.
  - **Agent 2 (Fit Analysis):** A brutal comparison of Master Profile gaps/strengths against the JD.
  - **Agent 3 (Cheat Sheet):** Business model deep dive, tech stack mapping, and cold outreach targets.
- **Interview Prep:** Cross-referencing the JD with my STAR stories to map out exact answers to the 5 most likely questions.

---

## 2. The Browser Fails (The 10-Minute Pivot)

To build a GUI, I initially went the default route: ClojureScript with Reagent, using **Scittle** to interpret it directly in the browser on GitHub Pages (`headhunter.nurazhar.com`). I used `typst.ts` (WASM) to compile PDFs in-browser.

It was fragile from day one. Web specs evolve, local storage is restrictive, and state-syncing inside a browser sandbox is prone to weird bugs. When `headhunter.nurazhar.com` failed to load, I didn't want to waste hours wrestling with browser console errors and JS dependency hell. 

The web stack is bloated. So, we deleted the web code and pivoted.

---

## 3. Native Desktop GUI with `cljfx`

Instead of the browser, I shifted to **`cljfx`**, a declarative wrapper for JavaFX.

Now, it is a standalone desktop application. No local web servers, no browser tabs, no JavaScript. It runs directly on the JVM, rendering native desktop windows on Arch/CachyOS.

Why this actually works:
1. **Zero Browser Bloat:** It is a compiled program, not a tab running in an engine.
2. **Immutable UI State:** The entire app state is inside a single Clojure atom. The UI is a pure function of the data. If the data is correct, the UI literally cannot render bugs.
3. **No Thread Freezing:** Heavy Gemini API calls run in Clojure `futures` (background threads), while `Platform/runLater` dispatches the results back to the main UI. The desktop app stays responsive at 60 FPS while the agents run.
4. **Local Privacy:** Everything is saved as local `.edn` files. No network syncing, no cloud databases, no trackers.

---

## Conclusion

We are conditioned to default to the web for everything. But for local-first utilities, the browser is just a bloated, insecure sandbox that gets in the way. 

Writing a native desktop app in Clojure is faster, runs better, and is vastly more stable. If your local tools are acting up because of browser quirks, stop trying to fix the JS bundle. Just delete it.

*Codebase:* **[headhunter-agent](https://github.com/nurazhardotcom/headhunter-agent)**
