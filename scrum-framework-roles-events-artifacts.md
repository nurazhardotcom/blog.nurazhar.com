Title: Scrum Framework — Roles, Events, and Artifacts for the Busy Engineer
Date: 2026-06-22
Tags: scrum, agile, project-management, methodology, lithan
Description: A focused, one-pager on the Scrum Framework: the three accountabilities, five events, three artifacts, empirical pillars, and five values. Real engineers explaining Scrum to engineers.

---

My last post explained why Waterfall failed and why Agile won. This post covers **Scrum** — the most widely used Agile framework — as a single, connected system.

If you're an engineer reading this: Scrum is not "standup meetings and sprints." It's a **feedback loop wrapped in a container, with clear accountability boundaries and explicit quality gates.** Every element exists because real teams needed it.

---

## What Scrum Is

From the 2020 Scrum Guide:

> Scrum is a lightweight framework that helps people, teams and organizations generate value through adaptive solutions for complex problems.

Scrum is:
- **Lightweight** — minimal rules (roles, events, artifacts, and their binding rules)
- **Simple to understand** but **difficult to master**
- A **framework**, not a methodology — it doesn't tell you how to test, deploy, or write code

It's built on **empirical process control**: knowledge comes from experience, decisions are based on what is known.

---

## The Three Pillars

```d2
# Diagram 160
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

Title: {
  label: "Empirical Process Control"
}

Transparency: {
  label: "Transparency\nWork and process\nmust be visible\nto all"
}

Inspection: {
  label: "Inspection\nFrequently examine\nartifacts and\nprogress toward\ngoals"
}

Adaptation: {
  label: "Adaptation\nAdjust the process\nor materials as\nsoon as deviations\nare detected"
}

Transparency -> Inspection: "You cannot inspect\nwhat you cannot see"
Inspection -> Adaptation: "Inspection without\nadaptation is\npointless"
Transparency -> Adaptation: "All three required\nto generate value"
```

Every Scrum event exists to enable one of these three pillars. The Daily Scrum inspects progress toward the Sprint Goal. The Sprint Review inspects the Increment. The Retrospective inspects the process itself. None of this works without **Transparency** — if your Product Backlog is hidden, your Sprint Backlog is unreadable, or your Definition of Done is vague, inspection is meaningless.

---

## The Five Values

Values give the pillars their foundation. When the team embodies these, empiricism works:

```d2
# Diagram 161
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

Title: {
  label: "Scrum Values"
}

Commitment: {
  label: "Commitment\nCommit to goals\nand each other"
}

Focus: {
  label: "Focus\nSprint work\nabove all else"
}

Openness: {
  label: "Openness\nWork and challenges\nare visible"
}

Respect: {
  label: "Respect\nTeam members are\ncapable and\nindependent"
}

Courage: {
  label: "Courage\nDo the right thing;\ntackle tough\nproblems"
}

Commitment -> Focus: "You commit, so\nyou focus"
Focus -> Openness: "Focus reveals\nchallenges \u2192 openness"
Openness -> Respect: "Openness requires\npsychological safety"
Respect -> Courage: "Respect gives you\ncourage to speak up"
Courage -> Commitment: "Courage lets you\ncommit honestly"
```

---

## The Three Accountabilities (Roles)

Scrum defines **three accountabilities**, not job titles:

```d2
# Diagram 162
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

Title: {
  label: "Scrum Team Accountabilities"
}

PO: {
  label: "Product Owner\nMaximizes value\nof the product"
}

Dev: {
  label: "Developers\nCreate the Increment\neach Sprint"
}

SM: {
  label: "Scrum Master\nEnsures Scrum is\nunderstood and\nenacted"
}

POBottom: {
  label: "Key: One person, not a committee.\nOwns the Product Backlog.\nDecides order and priority.\nEveryone respects their decisions."
}

DevBottom: {
  label: "Key: Cross-functional, self-managing.\n7\u00b12 people.\nNo sub-teams (no \"Devs + QA + DB\").\nCreates Sprint Backlog plan."
}

SMBottom: {
  label: "Key: Serves PO, Devs, and organization.\nCoach, facilitator, impediment remover.\nNot a project manager or team lead."
}

PO -> POBottom
Dev -> DevBottom
SM -> SMBottom
```

Key insight for engineers: **there is no "project manager" in Scrum.** The Product Owner decides *what* to build. The Developers decide *how* to build it. The Scrum Master ensures the process works.

---

## The Five Events

Events are **time-boxed** (shorter for shorter Sprints) and create **regularity** — minimizing the need for meetings not defined in Scrum:

```d2
# Diagram 163
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

Title: {
  label: "Scrum Events (1-month Sprint)"
}

SprintBig: "Sprint (\u22641 month)"

Sprint: {
  label: "The container.\nIdeas \u2192 value.\nNo changes that\nendanger Sprint\nGoal."
}

Plan: {
  label: "Sprint Planning\n(\u22648 hrs)\nWhy? \u2192 Sprint Goal\nWhat? \u2192 PBIs\nHow? \u2192 Plan"
}

DS: {
  label: "Daily Scrum\n(15 min)\nInspect Sprint\nGoal progress.\nDaily plan."
}

Review: {
  label: "Sprint Review\n(\u22644 hrs)\nInspect Increment.\nUpdate Product\nBacklog."
}

Retro: {
  label: "Sprint Retrospective\n(\u22643 hrs)\nInspect team.\nPlan improvements\nfor next Sprint."
}

SprintBig -> Plan
Plan -> DS: "Happens daily\nwithin Sprint"
DS -> Review
Review -> Retro
Retro -> SprintBig: "Next Sprint\nstarts immediately"
```

**Sprint** (the container):
- Fixed length ≤ 1 month
- A new Sprint starts immediately after the previous one ends
- Only the Product Owner can cancel a Sprint (and only if the Sprint Goal becomes obsolete)

**Sprint Planning** (the plan):
- Entire Scrum Team attends
- Answers: Why is this Sprint valuable? (Sprint Goal), What can be Done? (selected PBIs), How will the work be done? (Sprint Backlog plan)

**Daily Scrum** (the sync):
- 15 minutes, same time/place daily
- Developers inspect progress toward Sprint Goal
- Adapt Sprint Backlog as needed
- The Scrum Master enforces it happens, but **Developers own it**

**Sprint Review** (the demo):
- Scrum Team + stakeholders
- Inspect the Increment; not a presentation but a **working session**
- Discuss what was done, what changed, what to do next
- Product Backlog may be adjusted

**Sprint Retrospective** (the improvement):
- Team inspects itself (individuals, interactions, processes, tools, Definition of Done)
- What went well? What problems? How were they solved?
- Identify the most impactful changes for next Sprint

---

## The Three Artifacts and Their Commitments

Artifacts represent **work or value** and have explicit **commitments** that make them transparent:

```d2
# Diagram 164
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

Title: {
  label: "Scrum Artifacts"
}

PB: {
  label: "Product Backlog\nOrdered list of\nwhat's needed.\nEvolving, never\ncomplete."
}

PG: {
  label: "Commitment:\nProduct Goal\nLong-term target\nfor the product."
}

SB: {
  label: "Sprint Backlog\nSprint Goal +\nselected PBIs +\nplan to deliver."
}

SG: {
  label: "Commitment:\nSprint Goal\nThe single objective\nfor this Sprint.\nCreates focus."
}

Increment: {
  label: "Increment\nUsable, valuable\noutput of the\nSprint."
}

DoD: {
  label: "Commitment:\nDefinition of Done\nQuality gate.\nIf not met \u2192\nnot released."
}

PBOwner: {
  label: "Owner: Product Owner"
}

SBOwner: {
  label: "Owner: Developers"
}

IncOwner: {
  label: "Owner: Scrum Team"
}

PB -> PG: "commits to"
SB -> SG: "commits to"
Increment -> DoD: "committed to\nmeeting DoD"

PB -> PBOwner
SB -> SBOwner
Increment -> IncOwner
```

**Product Backlog** (managed by the Product Owner):
- Emergent, ordered list of everything needed
- Items that can be Done in one Sprint are "ready"
- Never complete — the Product Backlog lives as long as the product does
- **Commitment: Product Goal** — the long-term target that the backlog serves

**Sprint Backlog** (owned by the Developers):
- Sprint Goal + selected items + the plan to deliver them
- Highly visible, updated throughout the Sprint
- **Commitment: Sprint Goal** — the single reason this Sprint exists
- If the work turns out different than planned, the team negotiates scope, not quality

**Increment** (accountability of the whole Scrum Team):
- Sum of all completed Product Backlog items, integrated with prior work
- Must be **usable** (verifiable, valuable)
- **Commitment: Definition of Done** — a formal quality checklist
- If an item doesn't meet DoD, it goes back to the Product Backlog; it is NOT demoed or released

---

## How the System Connects

Here's the full framework as a single diagram:

```d2
# Diagram 165
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

Title: {
  label: "Scrum Framework \u2014 Complete System"
}

ProductGoal: {
  label: "Product Goal"
}

ProductBacklog: {
  label: "Product Backlog\nOrdered list of\nwhat's needed"
}

SprintPlanning: {
  label: "Sprint Planning"
}

SprintGoal: {
  label: "Sprint Goal"
}

SprintBacklog: {
  label: "Sprint Backlog\nGoal + items + plan"
}

Sprint: {
  label: "Sprint\n(\u22641 month)"
}

DailyScrum: {
  label: "Daily Scrum\n(15 min)"
}

Increment: {
  label: "Increment"
}

DoD: {
  label: "Definition\nof Done"
}

SprintReview: {
  label: "Sprint Review"
}

SprintRetro: {
  label: "Sprint Retro"
}

Note: {
  label: "Accountabilities:\nProduct Owner (backlog)\nDevelopers (plan + build)\nScrum Master (process)"
}

ProductGoal -> ProductBacklog: "informs"
ProductBacklog -> SprintPlanning: "inputs"
SprintPlanning -> SprintGoal: "defines"
SprintPlanning -> SprintBacklog: "creates"
SprintGoal -> SprintBacklog: "anchors"
SprintBacklog -> Sprint: "guides"
Sprint -> DailyScrum: "inspects"
Sprint -> Increment: "produces"
DoD -> Increment: "commits to\nquality gate"
Increment -> SprintReview: "inspected at"
SprintReview -> ProductBacklog: "may update"
SprintReview -> SprintRetro: "followed by"
SprintRetro -> SprintPlanning: "next Sprint"
```

The entire framework is a **closed feedback loop**:
1. **Product Goal** defines direction → **Product Backlog** captures what's needed
2. **Sprint Planning** selects work → **Sprint Goal** gives purpose → **Sprint Backlog** has the plan
3. **Sprint** executes → **Daily Scrum** keeps it on track → **Increment** is produced (must meet **Definition of Done**)
4. **Sprint Review** inspects the result → **Product Backlog** adjusts based on learning
5. **Sprint Retrospective** inspects the process → next **Sprint Planning** starts the cycle again

---

## Summary

| Element | What It Does | Timebox / Size |
|---------|-------------|----------------|
| **Product Owner** | Decides what to build | One person |
| **Developers** | Decides how to build | 7±2 people, cross-functional |
| **Scrum Master** | Ensures Scrum works | One person |
| **Sprint** | Container for work | ≤1 month |
| **Sprint Planning** | Selects work, defines goal | ≤8 hrs (1-month sprint) |
| **Daily Scrum** | Daily sync, adapt plan | 15 min |
| **Sprint Review** | Demo + feedback + re-prioritize | ≤4 hrs (1-month sprint) |
| **Sprint Retrospective** | Team improvement | ≤3 hrs (1-month sprint) |
| **Product Backlog** | Ordered work list | Living document |
| **Sprint Backlog** | Current sprint plan | Updated daily |
| **Increment** | Working, usable output | Every sprint |
| **Definition of Done** | Quality checklist | Team-defined |

Three pillars (Transparency, Inspection, Adaptation) + five values (Commitment, Focus, Openness, Respect, Courage) = the foundation that makes the framework work.

---

*Next: we map this framework directly onto improving this blog in production — the blog's Product Backlog, Sprint Planning, and first Sprint.*
