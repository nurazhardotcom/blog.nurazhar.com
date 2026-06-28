Title: Leaving GitHub for GitLab — A Post-Mortem on Platform Migration
Date: 2026-06-28
Tags: github, gitlab, migration, devops, self-hosting
Description: Why I moved my entire open-source presence from GitHub to GitLab — the trigger, the process, and what I kept public.
---

## The Trigger

Last week's [GitHub visibility disaster](github-repo-visibility-disaster.html) was the final push I needed. When an AI assistant's bad advice turned all my repos private, I spent 24 hours wrestling with GitHub's irreversible mechanics. I got them back, but the damage to my profile's continuity was done.

That incident made me ask a question I'd been avoiding: **why am I still on GitHub?**

## The Answer: I Shouldn't Be

I'd already written about [why GitLab is superior for solo developers](gitlab-superior-for-solo-devs.html). Built-in CI/CD, container registry, security scanning, unlimited private repos — all in one URL. My blog was already hosted on GitLab Pages. My CI pipelines already ran on GitLab runners.

But my repos were still on GitHub. Inertia, mostly.

## The Migration

I moved 7 repos to private and redirected my profile:

| Repository | Action |
|---|---|
| `aur-audit` | **Kept public** — supply chain security needs visibility |
| `bsv-clj` | **Kept public** — Bitcoin ecosystem reference |
| `headhunter-agent` | **Kept public** — open-source agent framework |
| `pdpa-sg-clj` | **Kept public** — compliance toolkit needs discoverability |
| `nurazhar.com` | Private |
| `nurazhardotcom` | Private — now a redirect README |
| `paperclip-clj` | Private |
| `lagu-lagu` | Private |
| `ipso-agent` | Private |
| `bsv-de-tracker` | Private |
| `agent-bond` | Private |

The 4 public repos stayed because they serve the ecosystem — people discovering them on GitHub should find them without a dead end.

My [profile README](https://github.com/nurazhardotcom/nurazhardotcom) now simply says:

> **I've moved to [gitlab.com/nurazhar](https://gitlab.com/nurazhar).** This repo is kept for archival purposes.

## The GitLab Advantage

Over the past week, everything consolidated to one place:

- **Repos** — `gitlab.com/nurazhar`
- **CI/CD** — Single `.gitlab-ci.yml`, no Actions tab needed
- **Pages** — Blog hosted on `nurazhar.com` via GitLab Pages
- **Container Registry** — Private images alongside the code
- **Security Scanning** — SAST, dependency scanning, license compliance — free

For a solo developer managing infrastructure, security tooling, and open-source projects, this collapses the toolchain entropy to near zero.

## The Long Tail

GitHub isn't gone — it's an archive. The 4 public repos still get issues and PRs there. The profile redirect points people to the active home. Old forks still resolve.

But the center of gravity has shifted. New projects land on GitLab first. The blog already lived there. The CI already ran there. The repos just caught up.

## What I Learned

1. **Platform lock-in is quiet** — you don't notice it until something breaks
2. **Migration is easier than it seems** — git remote set-url and you're done
3. **Profile redirects work** — a clear README and a link is enough
4. **Keep ecosystem repos public** — disappearing from the network hurts everyone

GitHub was the right place to start. GitLab is the right place to build.
