# Agent Guidelines for Homepage and Resume Management

- The git remote `origin` is GitLab. GitHub is auto-synced via GitLab push mirror — no manual push needed.
- Do not host or link to `resume.pdf` on the website.
- Use the LinkedIn profile link (`https://www.linkedin.com/in/in-azhar`) in the navigation bar instead of a resume PDF link.
- Run `bb build` to compile the static site templates.

## Deployment Workflow Reference

1. **Build and Validate**:
   ```bash
   bb build
   bb validate-links
   ```

2. **Verify Git Status**:
   ```bash
   git status
   git diff <changed-files>
   ```

3. **Commit and Push**:
   ```bash
   git add . && git commit -m "message" && git push origin main
   ```
   GitHub is auto-synced via GitLab push mirror. No separate push needed.

## Known Pitfalls — Read Before Publishing

1. **Run `bb build` locally before `git push`.** CI runs it on push (see `.gitlab-ci.yml`), but a local build catches frontmatter mis-parsing (bad `Date:` format, malformed `Tags:` list, missing `---` separator), `d2` syntax errors, and broken external links faster than waiting for CI to fail.

2. **Use `.html` extension instead of `.md` in relative links to other blog posts.** Source files are `.md` but compiled to `.html`. Linking to `.md` causes 404s on the live site.
