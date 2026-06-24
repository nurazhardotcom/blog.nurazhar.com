# Agent Guidelines for Homepage and Resume Management

- The user's personal website source code resides in `/home/nurazhar/Assistant/gitlab/homepage/`.
- The domain `nurazhar.com` points directly to GitLab Pages IP (`35.185.44.232`).
- The git remote `origin` pushes to both GitLab and GitHub repositories simultaneously. Always ensure code updates push to both targets.
- Do not host or link to `resume.pdf` on the website.
- Use the LinkedIn profile link (`https://www.linkedin.com/in/nur-azhar`) in the navigation bar instead of a resume PDF link.
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
