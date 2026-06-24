Title: Applying Agile and Scrum to a Solo Open-Source MAS Project — A Mentor's Guide
Date: 2026-06-24
Tags: agile, scrum, project-management, mas, clojure, ai-agents, architecture, lean, kanban
Description: A mentor's walkthrough on how to apply the Agile and Scrum principles from the PDWD-APM-0226 bootcamp to the headhunter-agent multi-agent system project, with D2 architecture diagrams.

---

*This post is written as a mentor guiding a mentee through the practical application of Agile Project Management concepts learned in the PDWD-APM-0226 course to a real-world project: the headhunter-agent.*

---

Hey — you just completed the Agile Project Management module, and you're looking at the **headhunter-agent** project going "how does this stuff actually apply?" Let's walk through it together.

The course covered Scrum roles, events, artifacts, Kanban, estimation, continuous improvement, and Lean principles. The headhunter-agent is a Local-First Multi-Agent System (MAS) written in Clojure — a desktop GUI + CLI toolchain for job hunting automation. Let me show you how every Agile concept maps to something real in this codebase.

## 1. Scrum Roles — Who Does What on a Solo Project?

The course asks: *What does a Scrum Master do to support teamwork and responsibility?* On a solo project, you wear all three hats. But they're still distinct responsibilities:

```d2
direction: right

Roles: "Scrum Roles\n(Solo Collapsed)" {
  PO: "Product Owner\nNur Azhar" {
    PO1: "Defines product vision"
    PO2: "Prioritises backlog"
    PO3: "Maximises value delivered"
    PO4: "Accepts or rejects work"
  }

  SM: "Scrum Master\nNur Azhar" {
    SM1: "Removes blockers"
    SM2: "Coaches Agile practices"
    SM3: "Facilitates events"
    SM4: "Protects from distraction"
  }

  DEV: "Development Team\nNur Azhar" {
    D1: "Self-organises work"
    D2: "Builds the Increment"
    D3: "Owns estimation"
    D4: "Delivers 'Done' each Sprint"
  }
}

PO -> SM: "aligns vision with process"
SM -> DEV: "enables execution"
DEV -> PO: "delivers value"
```

On headhunter-agent, here's how that breaks down:

| Role | Your Responsibility | Headhunter-Agent Example |
|------|-------------------|--------------------------|
| **Product Owner** | Decide what to build next | Prioritising M2M protocol P4 vs. adding a test suite |
| **Scrum Master** | Remove blockers, keep process healthy | Unblocking yourself when Gemini API rate-limits you |
| **Developer** | Write code, test, deliver | Implementing the 3-stage evaluator or the Daemon MCP server |

The **Scrum Master** differs from a traditional Project Manager because you don't *command and control* — you *serve the team* (which is yourself) by removing impediments. When you spend 3 hours debugging a DNS discovery issue, the SM in you asks: "What systemic fix prevents this from blocking next Sprint?"

## 2. Scrum Events — The Rhythm of Delivery

The course covers five Scrum events. Here's how they map when you're building the headhunter-agent:

```d2
direction: down

Sprint: "Sprint (1 week)\nMon → Sun" {
  Planning: "Sprint Planning\nMon 30min" {
    P1: "What can we deliver?"
    P2: "How will we do it?"
    P3: "Sprint Goal defined"
  }

  Daily: "Daily Scrum\nEvery morning 15min" {
    D1: "What did I do yesterday?"
    D2: "What will I do today?"
    D3: "Any blockers?"
  }

  Work: "Development Work" {
    W1: "Code, test, commit"
    W2: "Update tracker"
    W3: "Run bb build"
  }

  Review: "Sprint Review\nSun 30min" {
    R1: "Demo to stakeholder (you)"
    R2: "What was delivered?"
    R3: "Update backlog based on feedback"
  }

  Retro: "Sprint Retrospective\nSun 15min" {
    RT1: "What went well?"
    RT2: "What could improve?"
    RT3: "One actionable change"
  }
}

Sprint -> Planning: "start"
Planning -> Daily: "daily cadence"
Daily -> Work: "execution"
Work -> Review: "demo"
Review -> Retro: "inspect & adapt"
Retro -> Sprint: "next Sprint"
```

### Sprint Length
A one-week Sprint works well for a solo project. The headhunter-agent has clear module boundaries — you can ship something meaningful each week:

| Sprint | Goal | Deliverable |
|--------|------|-------------|
| 1 | Data Vault extraction | `profiler.clj` + `data/master-profile.edn` |
| 2 | Job evaluation pipeline | `evaluator.clj` — 3 sequential Gemini agents |
| 3 | Interview prep | `interview.clj` — STAR story mapping |
| 4 | PDF generation | `pdf.clj` + Typst template integration |
| 5 | Application tracker | `tracker.clj` — markdown pipeline |
| 6 | M2M key generation | `m2m/crypto.clj` — Ed25519 identity |
| 7 | M2M discovery + fetch | `m2m/registry.clj` + `m2m/fetch.clj` |
| 8 | M2M submit + verify | `m2m/submit.clj` + `m2m/verify.clj` |
| 9 | Daemon MCP server | `daemon/core.clj` + `daemon/data.clj` |
| 10 | Testing + hardening | Test suite + CI pipeline |

### Sprint Planning
Ask: *Given the current Product Backlog, what is the most valuable thing I can deliver this Sprint?* For Sprint 9, the answer was the Daemon MCP server — because it unlocks MCP protocol integration.

### Daily Scrum
Every morning answer three questions:
- **Yesterday:** "Implemented `tools/list` and `tools/call` handlers for the Daemon"
- **Today:** "Write the data content for all 9 tools, then wire up the HTTP server"
- **Blockers:** "Need to figure out babashka http-server handler syntax" → unblocked by reading `directory.clj`

### Sprint Review
At Sprint end, run the GUI and demo what you built. Record a screenshot or a short video. The review answers: *Does this actually solve the problem?*

### Sprint Retrospective
The most important event for a solo developer. The course asks *Why is continuous improvement important in Agile?* Here's why:

```d2
direction: down

Retro: "Sprint Retrospective" {
  Good: "What went well?" {
    G1: "Daemon server compiled first try"
    G2: "D2 diagrams rendered correctly"
  }

  Improve: "What could be better?" {
    I1: "Spent 2 hours on CSS instead of logic"
    I2: "No tests written again"
  }

  Action: "One change next Sprint" {
    A1: "Write at least one test per day"
    A2: "Use timebox (Pomodoro) for UI work"
  }
}

Good -> Action: "reinforce"
Improve -> Action: "address"
```

## 3. Scrum Artifacts — Tracking Value Delivered

The course identifies three artifacts: Product Backlog, Sprint Backlog, Increment. The headhunter-agent already has a markdown-based tracker — let's repurpose it as an Agile tool:

```d2
direction: down

PB: "Product Backlog\njds/* + tracker vision" {
  PB1: "M2M employer verify library (P5)"
  PB2: "Test suite (critical gap)"
  PB3: "CI pipeline in GitLab"
  PB4: "Gemini model selector in GUI"
  PB5: "Multi-language CV support"
  PB6: "LinkedIn auto-import"
}

SB: "Sprint Backlog\nTracked in data/applications.md" {
  SB1: "Daemon MCP server tools/list"
  SB2: "Daemon MCP server tools/call"
  SB3: "Data content for 9 tools"
  SB4: "Blog post with D2 diagrams"
}

Increment: "Increment\nShippable at Sprint end" {
  INC1: "bb daemon serve works"
  INC2: "curl can query get_architecture"
  INC3: "Blog post published"
}

PB -> SB: "Sprint Planning selects items"
SB -> Increment: "execution delivers value"
```

### Product Backlog
A living document of everything the headhunter-agent could become. Prioritised by value:

```d2
direction: down

Backlog: "Product Backlog (priority order)" {
  H1: "🔴 HIGH: Test suite (blocker)"
  H2: "🔴 HIGH: CI pipeline (blocker)"
  H3: "🟡 MED: Employer verify lib"
  H4: "🟡 MED: Multi-model support"
  H5: "🟢 LOW: LinkedIn auto-import"
  H6: "🟢 LOW: i18n CV templates"
}

H1 -> H2: "depends on"
H2 -> H4: "enables"
H3 -> H5: "future"
```

The **Definition of Done** for a backlog item on headhunter-agent:
- Code compiles without warnings
- `bb <command>` runs without errors
- D2 diagrams render correctly in the blog post
- Commit message follows conventional commits format
- README updated if behaviour changed

### Sprint Backlog
During Sprint Planning, pull items from the Product Backlog into the Sprint Backlog. Track progress in `data/applications.md`:

```
# | Date | Sprint | Item | Status | Notes
1 | 24-Jun | Sprint 9 | Daemon core.clj | ✅ Done | 159 lines, MCP compliant
2 | 24-Jun | Sprint 9 | Daemon data.clj | ✅ Done | 283 lines, 9 tools
3 | 24-Jun | Sprint 9 | deps.edn alias | ✅ Done | :daemon alias added
4 | 24-Jun | Sprint 9 | Blog post | ✅ Done | D2 diagrams included
```

### Increment
Every Sprint produces a **potentially shippable Increment**. For headhunter-agent, this means:
- CLI commands work (`bb daemon serve --port 8081`)
- GUI launches without JavaFX errors
- D2 diagrams compile in the blog post build

## 4. Kanban vs Scrum — When to Use Which

The course asks: *Compare Kanban and Scrum-based ceremonies. What is the purpose of each approach?*

For headhunter-agent, you can use **both**:

```d2
direction: right

Scrum: "Scrum (Development)" {
  S1: "Timeboxed Sprints"
  S2: "Fixed scope per Sprint"
  S3: "Role-based ceremonies"
  S4: "Good for: new features"
}

Kanban: "Kanban (Maintenance)" {
  K1: "Continuous flow"
  K2: "WIP limits"
  K3: "No fixed iterations"
  K4: "Good for: bug fixes, triage"
}

Board: "Kanban Board\n(for tracker)" {
  B1: "Backlog"
  B2: "In Progress"
  B3: "Review"
  B4: "Done"
}

Scrum -> Board: "feeds into"
Kanban -> Board: "feeds into"
```

### Scrum for Feature Development
Use Scrum when building a new module (like the Daemon). You need the structure of Sprints to stay focused.

### Kanban for the Application Pipeline
The tracker (`data/applications.md`) is essentially a Kanban board for your job applications. Each application flows through states:

```d2
direction: right

Pipeline: "Job Application Kanban" {
  Disc: "Discovered" {
    D1: "Found on MyCareersFuture"
    D2: "LinkedIn recommendation"
  }

  Eval: "Evaluated" {
    E1: "Score >= 4.0 → GO"
    E2: "Score < 4.0 → NO-GO"
  }

  Tailor: "Resume Tailored" {
    T1: "Gemini ATS optimization"
    T2: "Typst PDF compiled"
  }

  Submit: "M2M Submitted" {
    S1: "Ed25519 signed"
    S2: "Receipt acknowledged"
  }

  Done: "Resolved" {
    R1: "Interview scheduled"
    R2: "Offer / Rejected"
  }
}

Disc -> Eval: "bb evaluate"
Eval -> Tailor: "bb pdf"
Tailor -> Submit: "bb bb-m2m apply"
Submit -> Done: "tracker mark"
```

## 5. Agile Estimation — Story Points and Velocity

The course asks: *What are Agile estimation techniques, and how do they help in project planning?*

For headhunter-agent, use **Story Points** with a modified Fibonacci sequence (1, 2, 3, 5, 8, 13):

```d2
direction: down

Estimation: "Story Point Estimation" {
  S1: "1 pt — Trivial" {
    E1: "Fix typo in README"
    E2: "Update CSS colour"
  }

  S2: "2 pt — Simple" {
    E3: "Add new tool to Daemon"
    E4: "Update tracker format"
  }

  S3: "3 pt — Moderate" {
    E5: "Add new Gemini model option"
    E6: "Implement new agent stage"
  }

  S5: "5 pt — Complex" {
    E7: "Full M2M submit flow"
    E8: "GUI tab with live data"
  }

  S8: "8 pt — Uncertain" {
    E9: "DNS discovery edge cases"
    E10: "New Gemini integration"
  }

  S13: "13 pt — Epic" {
    E11: "Employer verify library"
    E12: "Full test suite"
  }
}

S1 -> S2 -> S3 -> S5 -> S8 -> S13: "increasing complexity"
```

### Velocity Tracking

Track your velocity over Sprints:

```d2
direction: down

V: "Velocity Chart" {
  Sprint1: "Sprint 1\n12 pts"
  Sprint2: "Sprint 2\n15 pts"
  Sprint3: "Sprint 3\n10 pts"
  Sprint4: "Sprint 4\n18 pts"
  Sprint5: "Sprint 5\n13 pts"
  Avg: "Average: 13.6 pts"
}

Sprint1 -> Sprint2: "+3"
Sprint2 -> Sprint3: "-5 (blocker: DNS)"
Sprint3 -> Sprint4: "+8"
Sprint4 -> Sprint5: "-5"
Sprint5 -> Avg: "forecast"
```

If your average velocity is ~14 pts per Sprint, you know you can plan ~14 pts in Sprint Planning. No more overcommitting.

## 6. Sprint Burndown — Are You on Track?

A Sprint Burndown chart shows remaining work vs. time. For headhunter-agent, track it in the markdown tracker:

```d2
direction: right

BD: "Sprint Burndown (Sprint 9)" {
  Mon: "Mon\n14 pts"
  Tue: "Tue\n11 pts"
  Wed: "Wed\n7 pts"
  Thu: "Thu\n3 pts"
  Fri: "Fri\n0 pts"
  Ideal: "Ideal line: -2.8 pts/day"
}

Mon -> Tue: "-3"
Tue -> Wed: "-4"
Wed -> Thu: "-4"
Thu -> Fri: "-3"
```

If by Thursday you still have 8 pts remaining, you know you overcommitted. Next Sprint, plan fewer points.

## 7. Agile Ceremonies — Best Practices

The course asks: *What are Agile ceremonies, and why are they important for team collaboration? List TWO best practices.*

On a solo project, ceremonies are mental resets, not meetings. Here are the four ceremonies mapped:

```d2
direction: down

Ceremonies: "Agile Ceremonies" {
  SP: "Sprint Planning\nMon 30min" {
    SP1: "Review Product Backlog"
    SP2: "Set Sprint Goal"
    SP3: "Select + estimate items"
    SP4: "Best practice: visualise with D2"
  }

  DS: "Daily Standup\n15min" {
    DS1: "3 questions"
    DS2: "Update tracker"
    DS3: "Best practice: timebox to 15min"
  }

  SR: "Sprint Review\n30min" {
    SR1: "Demo the Increment"
    SR2: "Run bb commands"
    SR3: "Show GUI screenshots"
    SR4: "Best practice: record the demo"
  }

  Retro: "Retrospective\n15min" {
    RT1: "Start/Stop/Continue"
    RT2: "One action item"
    RT3: "Best practice: write it down"
  }
}

SP -> DS: "daily"
DS -> SR: "end of Sprint"
SR -> Retro: "inspect"
Retro -> SP: "adapt"
```

## 8. Lean Principles — Eliminating Waste

The course asks: *How does the Lean principle of waste elimination support Agile continuous improvement?*

In headhunter-agent development, waste takes many forms:

```d2
direction: right

Waste: "Seven Wastes of Software" {
  W1: "Partially Done Work\nStale branches, half-finished features"
  W2: "Extra Features\nGold-plating the GUI instead of shipping"
  W3: "Relearning\nNot documenting architecture decisions"
  W4: "Handoffs\nWaiting for Gemini API reviews"
  W5: "Task Switching\nContext-switching between GUI and CLI"
  W6: "Delays\nGemini API rate limit backoffs"
  W7: "Defects\nBugs found in Sprint Review"
}

Lean: "Lean Eliminations" {
  L1: "Continuous Integration → no stale code"
  L2: "MVP thinking → no gold-plating"
  L3: "Architecture Decision Records → no relearning"
  L4: "Automated testing → no manual regression"
  L5: "Focused Sprints → no task switching"
  L6: "Local-first design → no API dependency delays"
  L7: "Definition of Done → no defects shipped"
}

Waste -> Lean: "eliminate"
```

Concrete example: The headhunter-agent already eliminates waste #6 (Delays) by being **local-first** — Gemini API calls are the only external dependency. The Daemon MCP server eliminates waste #3 (Relearning) by exposing system documentation through a standard protocol — any AI agent can query the architecture instead of you re-explaining it.

## 9. Continuous Improvement in Practice

The course asks: *Why is continuous improvement important in Agile? Explain THREE key benefits.*

Let me show you how three benefits play out in headhunter-agent:

### Benefit 1: Enhanced Product Quality

Each retrospective identifies quality gaps. Sprint 8's retro identified "no tests" as a critical gap. Sprint 9's action item: "Write at least one test per day." This directly improves the quality of the M2M crypto module.

### Benefit 2: Increased Team Efficiency

The `tracker.clj` module went through three iterations:
- **Iteration 1:** Manual markdown editing (wasteful)
- **Iteration 2:** `bb tracker add --company X` CLI command (better)
- **Iteration 3:** GUI tab with visual cards (efficient)

Each improvement came from a retrospective insight.

### Benefit 3: Fostering Innovation

The Daemon MCP server was not in the original Product Backlog. It emerged from a Sprint Review where you realised: "I keep re-explaining the architecture to AI agents. What if the project could answer for itself?" That's continuous improvement driving innovation.

```d2
direction: down

CI: "Continuous Improvement Cycle" {
  Code: "Build\nWrite code, ship feature"
  Review: "Inspect\nSprint Review + Retro"
  Learn: "Learn\nIdentify patterns, document"
  Improve: "Adapt\nOne change next Sprint"
}

Code -> Review: "end of Sprint"
Review -> Learn: "what did we learn?"
Learn -> Improve: "actionable insight"
Improve -> Code: "next Sprint"
```

## 10. Putting It All Together — Your Sprint Template

Here's a concrete template for running your next Sprint on headhunter-agent:

```d2
direction: down

Template: "Sprint X Template" {
  Goal: "Sprint Goal:\n[one sentence, e.g. Ship M2M submit flow]"

  Backlog: "Sprint Backlog:" {
    B1: "[ ] 5pts — Implement submit.clj multipart builder"
    B2: "[ ] 3pts — Wire up POST to employer endpoint"
    B3: "[ ] 2pts — Parse acknowledgment response"
    B4: "[ ] 3pts — Update tracker integration"
    B5: "[ ] 1pt  — Blog post with D2 diagrams"
  }

  Total: "Total: 14 pts (matches avg velocity)"

  Events: "Events:" {
    E1: "Daily: 15min standup"
    E2: "Review: Demo bb bb-m2m apply"
    E3: "Retro: Start/Stop/Continue"
  }

  Done: "Definition of Done:\n- Code compiles\n- bb command works\n- Git push to GitLab"
}

Goal -> Backlog: "selects items"
Backlog -> Total: "sum"
Total -> Events: "plan"
Events -> Done: "deliver"
```

## Key Takeaways for Your Mentee

1. **Scales down, not up.** All Scrum roles, events, and artifacts work for a solo developer. You just collapse the roles into one person and timebox the events aggressively.

2. **The tracker is your backlog.** `data/applications.md` is already a Kanban board. Add a `Sprint` column and you have a complete Agile project management tool in markdown.

3. **D2 diagrams are your velocity chart.** Every blog post documents a Sprint. The D2 diagrams serve as Sprint Review documentation — visual proof of what shipped.

4. **The Daemon is your retrospective output.** The MCP server exists because you identified waste (#3 — relearning) and eliminated it by making the project self-documenting.

5. **Continuous improvement is real.** Three Sprints ago, the headhunter-agent had no tests, no CI, and no Daemon. Now it has all three. That's not magic — that's one actionable change per retrospective, compounded.

## Repository

The full source is at **[gitlab.com/nurazhar/headhunter-agent](https://gitlab.com/nurazhar/headhunter-agent)**. The Product Backlog is whatever issue you open next. The Sprint starts when you decide to ship something. Go build.
