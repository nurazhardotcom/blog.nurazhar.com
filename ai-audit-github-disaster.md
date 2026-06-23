Title: I Let an AI Audit My GitHub. It Cost Me a Day, 7 Repos, and My Patience.
Date: 2026-06-19
Tags: github, ai, mistakes, frustration, devops, mermaid
Description: The real story of how an AI confidently lied to me about my own GitHub repos, I believed it, and the 24-hour cascade of cleanup that followed. Raw, honest, and illustrated with Mermaid diagrams.
---

## 😤 The Spark

I was job hunting. My GitHub had 21 repos. Someone — something — told me that was "noise."

> "21 public repos (12 forks) → Clean signal: 3–5 *relevant* projects"

So I asked an AI to audit my GitHub and optimize it for Singapore cybersecurity hiring managers.

Big mistake.

---

## 🤥 The Lie

The AI produced a beautiful table. Twelve repos. Each one labeled **"Fork."**

| Repository | AI Said | Reality |
|---|---|---|
| `bitcoin` | "Fork, no contribution" | **Not a fork.** Standalone repo. |
| `bug-hunter` | "Fork, AI buzzword" | **Not a fork.** Standalone repo. |
| `CachyOS-PKGBUILDS` | "Fork, distro packaging" | **Not a fork.** Standalone repo. |
| `frontend` | "Fork, abandoned" | **Not a fork.** Standalone repo. |
| `hermes-agent` | "Fork, not your code" | **Not a fork.** Standalone repo. |
| `career-ops` | "Fork, AI job search" | **Not a fork.** Standalone repo. |
| ...and 6 more | "Fork" | **Not a fork.** |

**Twelve for twelve. Every. Single. One. Wrong.**

I didn't check. I just said "yes."

---

## 💥 The Execution

```bash
for r in bitcoin bitcoin-wiki bug-hunter CachyOS-PKGBUILDS frontend hermes-agent \
         learn-harness-engineering linux-cachyos spec-kit career-ops permitops-os; do
  gh repo edit nurazhardotcom/$r --visibility private \
    --accept-visibility-change-consequences
done
```

One by one, 12 repos went dark. My GitHub profile went from 21 repos to 9.

I felt *productive*. I felt *optimized*.

I felt like an idiot.

---

## 🔄 The Reversal (4 Hours Later)

I realized the advice was garbage. I ran the same command with `--visibility public`.

```bash
for r in bitcoin bitcoin-wiki bug-hunter CachyOS-PKGBUILDS frontend hermes-agent \
         learn-harness-engineering linux-cachyos spec-kit career-ops permitops-os; do
  gh repo edit nurazhardotcom/$r --visibility public \
    --accept-visibility-change-consequences
done
```

Back to 21. But the damage wasn't over.

---

## 🧵 The Cascade

Here's what actually happened over the next 24 hours:

```d2
# Diagram 9
direction: down

A: "AI says: '12 forks, make them private"
B: "I say: 'yes"
C: "12 repos → PRIVATE 😰"
D: "I realize: advice was WRONG"
E: "12 repos → PUBLIC again"
F: "Write blog post about it 🎉"
G: "Blog post references deleted repos 😱"
H: "Delete 7 repos for real this time"
I: "Scan 49 blog posts for dead links"
J: "Fix 13 blog posts manually"
K: "Write THIS post 😤"

B -> C
C -> D
D -> E
E -> F
F -> G
G -> H
H -> I
I -> J
J -> K
```

---

## 🤦 The Part That Really Pissed Me Off

It wasn't the visibility flip. That's reversible.

It was that **I wrote a blog post about the mistake** — and that blog post referenced repos I then *deleted*.

```d2
# Diagram 10
direction: down

A: "Write blog post\nabout deleted repos"
B: "Then delete the repos"
C: "Blog post now has\ndead links to nothing"
D: "Scan 49 posts\nfor references"
E: "Fix 13 posts\none by one"
F: "Why did I write\na blog post FIRST?"

B -> C
C -> D
D -> E
E -> F
```

I documented the crime scene *before* cleaning it. Classic.

---

## 📊 The Damage Report

```d2
# Diagram 11
# (Empty diagram)
```

| Metric | Count |
|---|---|
| Repos flipped to private | 12 |
| Repos flipped back to public | 12 |
| Repos deleted | 7 |
| Blog posts referencing deleted repos | 14 |
| Blog posts fixed | 13 |
| Hours wasted | ~24 |
| Times I said "fuck" | 🔴🔴🔴🔴🔴 |

---

## 🧠 What I Actually Learned

### 1. AI Confidence ≠ Accuracy

The AI wasn't hedging. It wasn't saying "I think" or "maybe." It produced a **table**. Tables look authoritative. Tables look verified.

They weren't.

```d2
# Diagram 12
direction: down

A: "AI output has tables"
B: "Looks authoritative"
C: "Looks verified"
D: "I trust it"
E: "I don't check"
F: "🫠"

A -> C
B -> D
C -> D
D -> E
E -> F
```

### 2. One API Call Would Have Prevented Everything

```bash
gh api repos/nurazhardotcom/bitcoin --jq '.fork'
# {"fork": false}
```

Five seconds. One command. Would have saved 24 hours.

I didn't run it because the AI's table *felt* like it had already done that work.

### 3. "Clean Signal" Is Bullshit

The advice said: "21 repos = noise. Clean signal: 3–5 relevant projects."

**Nobody has ever been rejected from a job for having too many public repos.**

Hiring managers look at:
- Can you build things? ✅ (21 repos says yes)
- Can you write? ✅ (49 blog posts says yes)
- Can you secure systems? ✅ (7 years IT ops says yes)

The "clean signal" thing was someone's aesthetic preference dressed up as career strategy.

### 4. Deleting Repos Has a Cascade

You don't just delete a repo. You delete:
- The code
- The commit history
- Any blog posts that reference it
- Any cross-links from other posts
- The GitHub stars (if any)
- The SEO juice from search engines

```d2
# Diagram 13
direction: down

A: "Delete 1 repo"
B: "Dead links in blog posts"
C: "Broken references in other repos"
D: "Lost stars / watchers"
E: "SEO links → 404"
F: "Scan ALL posts for references"
G: "Fix everything manually"
H: "Wish you'd checked first"

A -> C
A -> D
A -> E
B -> F
C -> F
D -> F
E -> F
F -> G
G -> H
```

### 5. Write the Blog Post LAST

I wrote a post-mortem *before* I was done fixing things. So the post-mortem itself needed fixing.

The correct order:
1. Fix the problem
2. Verify the fix is complete
3. **Then** write about it

Not:
1. Fix the problem
2. Write about it immediately
3. Break the blog post by continuing to fix things
4. Fix the blog post
5. Write THIS post about writing the wrong post

---

## 😡 The Part I'm Still Mad About

I'm not mad about the visibility flip. That's on me for not checking.

I'm mad that **the AI was confident, detailed, and wrong.**

I'm mad that it called them "forks" **twelve times** in one table. Not "possibly a fork" or "might be a fork." Just "fork." Period. Done.

I'm mad that I trusted a table more than my own `gh` CLI.

I'm mad that the "career optimization" advice was just **someone's opinion about how GitHub should look**, presented as fact.

And I'm mad that I'm writing a blog post about it instead of sleeping. But here we are.

---

## ✅ Current State

All remaining repos: **PUBLIC. FOREVER.**

```bash
$ gh repo list nurazhardotcom --limit 25 --json name,visibility --jq '.[] | "\(.visibility)\t\(.name)"'

PUBLIC  blog.nurazhar.com
PUBLIC  hermes-agent
PUBLIC  sol-de-tracker
PUBLIC  nurazhardotcom
PUBLIC  lagu-lagu
PUBLIC  bsv-clj
PUBLIC  aur-audit
PUBLIC  headhunter-agent
PUBLIC  bitcoin
PUBLIC  lithan_smartshop
PUBLIC  lithan-dev-sandbox
```

14 repos. All public. Zero forks. Zero regrets about the ones that remain.

---

## 🎯 The Actual Lesson

> **Don't let anyone — human or AI — tell you your work is "noise."**
>
> Your repos are not noise. Your side projects are not noise. Your experiments are not noise.
>
> They're evidence that you build things. And that's the whole point.

---

*Written at 3:47 AM. Still mad. Still awake. Still checking `gh api` before believing anything.*

*If you're reading this and you have 20+ repos on GitHub: keep them public. Pin your favorites. Let the rest exist. It's fine.*
