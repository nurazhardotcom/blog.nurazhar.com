# Agent Guidelines for Homepage and Resume Management

- The user's personal website source code resides in `/home/nurazhar/Assistant/gitlab/homepage/`.
- The domain `nurazhar.com` points directly to GitLab Pages IP (`35.185.44.232`).
- The git remote `origin` pushes to both GitLab and GitHub repositories simultaneously. Always ensure code updates push to both targets.
- Do not host or link to `resume.pdf` on the website.
- Use the LinkedIn profile link (`https://www.linkedin.com/in/in-azhar`) in the navigation bar instead of a resume PDF link.
- Run `bb build` inside `/home/nurazhar/Assistant/gitlab/homepage/` to compile the static site templates.

## Deployment Workflow Reference

When modifying site configurations, templates, or blog posts, execute the following commands to build the site and deploy the changes to both repositories:

1. **Verify Git Status and Diff**:
   ```bash
   git status
   git diff src/site/fabricate/dev/dev.clj
   ```

2. **Stage, Commit, and Deploy to GitLab**:
   ```bash
   git add . && git commit -m "feat: replace resume link with linkedin link and remove resume copying" && git push origin main
   ```

3. **Deploy to GitHub Mirror**:
   ```bash
   git push git@github.com:nurazhardotcom/nurazhar.com.git main
   ```

## Known Pitfalls — Read Before Publishing

Encountered during past runs. These are the failure modes future agents will hit if the workflow above is followed blindly:

1. **`code-reviewer-minimax-m3` can fail with internal tool errors** (e.g. `str_replace` unavailable in the subagent's runtime). When this happens, do NOT publish without one of:
   - A successful reviewer pass on the diff, OR
   - A clean local `bb build` that renders the post correctly, OR
   - An explicit human review note in the commit message.
   Silently pushing on a failed reviewer pass leaves typos or wrong claims live on `nurazhar.com`.

2. **Run `bb build` locally before `git push` for blog-post edits.** CI runs it on push (see `.gitlab-ci.yml`), but a local build catches frontmatter mis-parsing (bad `Date:` format, malformed `Tags:` list, missing `---` separator), `d2` syntax errors, and broken external links faster than waiting for CI to fail.

3. **The diff in Deployment Workflow step 1 only applies to template / site-config changes.** For blog-post edits you only touch root-level `*.md` files, not `src/site/fabricate/dev/dev.clj`. Substitute `git diff -- '*.md'` or `git diff <new-post>.md` to confirm the frontmatter and prose before committing.

4. **Both pushes are mandatory, even though the intro line claims origin pushes to both.** In this repo, `origin` is the GitLab remote only — the GitHub mirror is a separate URL. Always run `git push origin main` and `git push git@github.com:nurazhardotcom/nurazhar.com.git main`. Skipping the second push leaves GitHub behind GitLab.
