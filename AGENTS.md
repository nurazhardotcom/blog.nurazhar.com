# Blog conventions

- Write in D2, output SVG. Skip PNG entirely. No Playwright needed.
- H1 should be conversational, not academic
- D2 SVG image right below title
- Open with a personal hook (2-3 sentences)
- Use tables for comparisons
- Short sections with bold subheads
- Short paragraphs (1-4 lines max)
- Bold key terms for scanability
- No TOC, no footnotes, no fluff
- End with a one-liner or takeaway

# Git workflow

- Push to `origin` (GitLab) only — GitHub mirror is automatic via GitLab push mirror.
- Run `bb build` and `bb validate-links` before committing.
- Use `--no-verify` only if pre-commit hook blocks on pre-existing broken links (not your fault).
