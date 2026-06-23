Title: What Is a GitHub Fork? (And Why Mine Was Broken)
Date: 2026-06-20
Tags: git, github, opensource, fork, contribution
Description: I tried to contribute a feature to Hermes Agent and discovered my repo wasn't actually a fork. Here's what I learned about how GitHub forks work -- and why "fork" on GitHub doesn't mean what I thought it meant.

---

## The Confusion

I built a [bulk session archive feature](/hermes-desktop-bulk-session-archive.html) for Hermes Desktop. To contribute it back to the project, I needed to create a pull request to `NousResearch/hermes-agent`.

But GitHub said: *"There isn't anything to compare."*

I had a repo called `nurazhardotcom/hermes-agent` with the code, the branch, and the commits. It should work, right?

Wrong. My repo was **not a fork**. It was a standalone copy.

I didn't understand what a GitHub fork actually was. Here's what I learned.

## Traditional Fork vs GitHub Fork

When I hear "fork," I think of real divergences — projects that split and became independent. Let me show you three examples.

### Linux Distributions

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
```

Ubuntu forked Debian. They went separate ways — different package managers (apt vs dpkg directly), different release cycles, different communities. The fork created **two independent projects** that never merge back.

### Bitcoin

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
```

**Critical correction:** The common narrative says "BTC = Bitcoin's whitepaper." That's wrong.

- **Satoshi's whitepaper (2008):** Peer-to-peer electronic cash. Unbounded on-chain scaling. Sub-cent fees. SPV.
- **Bitcoin v0.1 (2009):** The original implementation that matched the whitepaper.
- **BTC (today):** Added SegWit (2017), Taproot (2021), pushed transactions to Lightning L2. These are **protocol changes that deviate from the whitepaper.** Blocks are no longer "unbounded." Fees are not sub-cent.
- **Bitcoin Cash (BCH):** Forked BTC over the block size debate but kept SegWit.
- **Bitcoin SV (BSV):** Forked BCH to restore the original protocol — unbounded on-chain scaling, no artificial block caps, sub-cent fees, SPV. **Nearest to Satoshi's whitepaper and v0.1 design.**

Satoshi's famous quote: *"The design supports a tremendous variety of possible transaction types... the system is not secure if fully anonymous, but the original Bitcoin software is."* BSV follows this. BTC moved to a different vision.

BSV is not a departure from Bitcoin. BSV **is** Bitcoin — the protocol as originally designed.

### The Pattern

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
```

Every traditional fork follows this pattern:
1. **Split** — copy the code, go your own way
2. **Diverge** — different features, different direction
3. **Independent** — no merging back, no communication

That's what I thought a GitHub fork was. And that's why I didn't want one. I didn't want to be Bitcoin Cash. I wanted to contribute to Bitcoin.

## What a GitHub Fork Actually Is

A GitHub fork is the **opposite** of a divergence. It's a **link that points back to the original**.

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
```

A GitHub fork is like a **branch that lives in your own namespace**. You can push to it freely, but the original project can still see it and accept changes from it.

The key difference: **GitHub knows the two repos are connected**.

## What I Actually Did

I created a **standalone repo** and pushed code to it. From the outside, it looked the same. But GitHub saw this:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
```

GitHub had no idea these two repos were related. Same commits, same code — but no link. When I tried to open a PR from my repo to upstream, GitHub said:

> *"There isn't anything to compare. We couldn't figure out how to compare these references."*

Because GitHub's PR system works by comparing branches **across linked repos**. No link = no comparison = no PR.

## The Three Ways This Can Go

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
```

### Option 1: Delete & Re-Fork (Cleanest)

Delete `nurazhardotcom/hermes-agent`. Then use GitHub's "Fork" button on `NousResearch/hermes-agent` to create a proper linked fork with the same name. Push your feature branch. Open the PR.

**Pros:** Clean. Same repo name. PR works natively.
**Cons:** Lose the existing repo (stars, watchers, any history — though you have none on this one).

### Option 2: Rename & Re-Fork (Safely)

Rename your current repo to `nurazhardotcom/hermes-agent-backup`. Fork `NousResearch/hermes-agent` into the now-free `nurazhardotcom/hermes-agent` name. Push your branch. Open the PR.

**Pros:** Keep your old repo around.
**Cons:** Two repos to manage.

### Option 3: Keep As-Is (No Upstream)

Leave everything where it is. Your feature lives in your repo only.

**Pros:** Nothing to fix.
**Cons:** You can't contribute to upstream. No PR possible.

## The Difference in One Table

| Concept | Traditional Fork (Ubuntu, BCH) | GitHub Fork |
|---|---|---|
| **Mechanism** | Copy code, diverge permanently | GitHub "Fork" button creates linked copy |
| **Relationship** | Becomes independent immediately | Stays connected to parent ("forked from...") |
| **Direction** | One-way departure | Two-way: pull from upstream, PR back |
| **Merging back** | Never re-merges | Designed for PRs back to original |
| **Communication** | Ceases after fork | "Fetch upstream" keeps you synced |
| **Network effect** | Fragmented — N isolated projects | Consolidated — contributors cluster on one upstream |
| **Goal** | Become your own project | Contribute to the original |
| **Real examples** | Ubuntu, BCH, BSV, CentOS — all diverged permanently | Your fork of NousResearch/hermes-agent |
| **Bitcoin analogy** | BTC added SegWit/Taproot (drifted from whitepaper). BSV restored the original protocol. Neither merges back. | You fork, you contribute back via PR. The whole point is re-merging. |
| **Metaphor** | Bitcoin Cash leaving Bitcoin — permanent departure | A branch on a different remote — designed to merge back |

A GitHub fork is really a **branch that lives in your own namespace**. Not a departure.

## What "Fetch Upstream" Means

On a proper GitHub fork, GitHub shows a "Fetch upstream" button:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
```

This syncs the latest changes from the original project into your fork. Your fork stays up-to-date with upstream while you work on your feature branch. **This is why a GitHub fork is "contribute" not "diverge."**

## What I'll Do

Option 1. Delete the standalone. Create a proper fork. Push the feature branch. Open the PR.

The bulk archive feature is 3 files, 106 lines, tests included. It's a contribution worth making properly.

---

*Next post: the actual PR workflow — creating the issue, implementing the feature, and getting it merged.*
