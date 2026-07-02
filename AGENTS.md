# Blog Conventions

- Write in D2, output SVG. Skip PNG entirely. No Playwright needed.
- H1 should be conversational, not academic.
- D2 SVG image right below title.
- Open with personal hook (2-3 sentences).
- Use tables for comparisons.
- Short sections with bold subheads.
- Short paragraphs (1-4 lines max).
- Bold key terms for scanability.
- No TOC, no footnotes, no fluff.
- End with one-liner or takeaway.

# Git Workflow

- Push to `origin` (GitLab) only — GitHub mirror is automatic via GitLab push mirror.
- Run `bb build && bb test && bb validate-links` before committing (also wired into `.git/hooks/pre-commit`, but a manual run catches failures faster than waiting for the hook to abort).
- Use `bb bench` to time a warm local build before quoting build / pipeline numbers in a blog post. Manual `time` measurements drift over time as the corpus grows and d2 SVG complexity shifts. The task is the audit trail.
- Use `--no-verify` only if pre-commit hook blocks on pre-existing broken links (not your fault).

# CI

- `pages` job runs on a **self-hosted** runner (`shell` executor, tag `homepage-self-hosted`). GitLab shared runners are not tagged for the job and never pick it up.
- Self-hosted runners do **not** consume GitLab.com's 400-min free-tier compute quota — the quota only applies to GitLab-managed shared runners.
- Pipeline: `bb build` → `bb validate-links` → upload `public/` artifact. No docker dependency — `bb`, `pandoc`, `d2`, `make` are installed natively on the runner host.
- The runner is intentionally the **only** path for the `pages` job — if the runner host is offline, jobs queue indefinitely (no shared-runner fallback by design).
- Register/install commands and the architecture rationale live in [`local-gitlab-runner-unlimited-ci.md`](local-gitlab-runner-unlimited-ci.md).
