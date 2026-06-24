Title: Debugging GitLab Pages: Hardcoded Paths, HTTPS Domains, and Self-Healing CI/CD
Date: 2026-06-24
Tags: gitlab, pages, cicd, devops, debugging, babashka, d2, ssl
Description: Two issues broke my CI/CD pipeline and HTTPS domain on nurazhar.com: a hardcoded local resume path in the static site generator, and an incomplete GitLab Pages SSL certificate for the custom domain. Here's the forensic breakdown.

---

## Two Problems, One Morning

This morning I pushed a blog post about the M2M Job Application Protocol — 23 D2 diagrams, 740 lines of markdown, the whole thing. The build succeeded locally. I committed. I pushed. And then:

1. **The blog post never appeared.** The GitLab CI/CD pipeline had silently failed.
2. **`https://nurazhar.com/` didn't work.** TLS handshake completed but the connection hung.

Two unrelated issues, same root cause: assumptions about the build environment.

## Problem 1: The Hardcoded Resume Path

The Fabricate SSG (our 253-line Babashka static site generator) had one line that only worked on my laptop:

```d2
direction: right
dev: dev.clj:235 {
  shape: code
  style.font: mono
  label: |md
    `(io/copy (io/file "/home/nurazhar/Assistant/.../Resume.pdf")
              (io/file "public/resume.pdf"))`
  |
}
CI: GitLab CI Runner {
  shape: hexagon
  style.fill: "#d32f2f"
}
laptop: My Laptop {
  shape: hexagon
  style.fill: "#2e7d32"
}

laptop -> dev: file exists ✅
CI -> dev: file not found ❌
```

### The Error

In the GitLab CI runner (Alpine Linux Docker container), the build script tried to copy a file at an absolute path that only exists on my local machine:

```
java.io.FileNotFoundException:
  /home/nurazhar/Assistant/Lifestyle Design Coach/Job Hunting/...
  Supabase_IT_Systems_Administrator_Resume.pdf (No such file or directory)
```

The entire build crashed. No HTML files were generated. GitLab Pages served the stale deployment from the previous successful pipeline.

### The Fix

The fix is defensive: check if the file exists before copying, and continue gracefully if it doesn't. This is a one-line change with a three-line guard:

```d2
direction: down
Before: Before (fragile) {
  shape: code
  style.font: mono
  style.fill: "#d32f2f"
  label: |md
    `(io/copy (io/file "/home/.../resume.pdf")
              (io/file "public/resume.pdf"))`
  |
}
After: After (resilient) {
  shape: code
  style.font: mono
  style.fill: "#2e7d32"
  label: |md
    ```
    (let [f (io-file "/home/.../resume.pdf")]
      (if (.exists f)
        (io/copy f (io/file "public/resume.pdf"))
        (println "⚠️  Resume not found, skipping.")))
    ```
  |
}
Before -> After: defensive check
```

The principle: **a static site generator should never crash because a non-essential asset is missing.** The resume PDF is a convenience copy, not a build-critical artifact. CI builds skip it; local builds include it.

## Problem 2: HTTPS on the Custom Domain

The second issue was invisible until I looked at the response headers:

```d2
direction: down
GitLab: nurazhar.gitlab.io/homepage/ {
  shape: hexagon
}
nurazhar: nurazhar.com {
  shape: hexagon
}
redirect: 308 Redirect {
  shape: code
  style.font: mono
}
broken: HTTPS hangs after TLS handshake {
  shape: code
  style.fill: "#d32f2f"
}

GitLab -> redirect: 308 to http://nurazhar.com/
redirect -> nurazhar: http:// → 200 OK ✅
nurazhar -> broken: https:// → timeout ❌
```

### The Mechanism

GitLab Pages has a project-level setting for custom domains. When you set `nurazhar.com` as the custom domain, GitLab automatically:

1. Issues a Let's Encrypt SSL certificate for the domain
2. Redirects all traffic from the default GitLab Pages URL to the custom domain
3. Handles both HTTP and HTTPS on the custom domain

The 308 redirect from `nurazhar.gitlab.io/homepage/` to `http://nurazhar.com/` works. But HTTPS on `nurazhar.com` hangs because **the SSL certificate verification wasn't completed**.

### GitLab Pages SSL Flow

```d2
direction: down
Setup: Add domain in GitLab Pages settings
DNS: Add DNS TXT verification record
Verify: GitLab verifies domain ownership
Cert: Let's Encrypt issues certificate
HTTPS: HTTPS works

Setup -> DNS
DNS -> Verify
Verify -> Cert
Cert -> HTTPS
```

The missing step was likely the DNS TXT verification record. GitLab Pages requires you to add a TXT record to your domain's DNS to prove ownership before it issues the SSL certificate. Without it, Let's Encrypt can't complete the challenge, and HTTPS stays broken.

### The Fix

This needs to be fixed in **GitLab project settings**, not in code:

1. Go to your GitLab project → Settings → Pages
2. Verify the custom domain `nurazhar.com` is listed
3. If SSL status shows "not verified", add the DNS TXT record to your domain
4. Wait for Let's Encrypt to issue the certificate
5. Check: `curl -sI https://nurazhar.com/` should return `200 OK`

DNS A records must point to GitLab Pages IPs:

```d2
direction: right
DNS: DNS Zone
A1: A 35.185.44.232
A2: A 35.227.127.127
A3: A 35.227.31.254
A4: A 35.227.52.231
AAAA: AAAA 2600:1900:4190::|
TXT: TXT _gitlab-pages-verification.xxx

DNS -> A1
DNS -> A2
DNS -> A3
DNS -> A4
DNS -> AAAA
DNS -> TXT
```

## Lessons Learned

### 1. Never Hardcode Absolute Paths in Build Scripts

Any path that starts with `/home/` is a ticking time bomb. Build scripts should:

- Accept paths via environment variables or CLI args
- Check file existence before operating
- Degrade gracefully when optional files are missing

### 2. CI/CD ≠ Local Dev Environment

The rules are simple but easy to forget:

```d2
direction: right
local: Local Build {
  shape: hexagon
}
ci: CI Build {
  shape: hexagon
}

local -> ci: differences:
Diff1: Filesystem layout
Diff2: Environment variables
Diff3: Network access
Diff4: Available tools
```

My laptop has the resume PDF. The CI runner doesn't. My laptop has environment variables in `.env`. The CI runner has them in GitLab CI variables. The fix is to **simulate the CI environment locally** before pushing.

### 3. HTTPS is Not Automatic

GitLab Pages says "automatic SSL" but there's a manual DNS verification step:

- Adding the custom domain in GitLab settings creates a verification TXT record value
- You must add this TXT record to your DNS zone
- Only then does Let's Encrypt issue the certificate
- The process is not instantaneous — DNS propagation takes minutes to hours

### 4. The Diagnostic Toolkit

Here's the checklist I ran through:

```d2
direction: down
check1: curl -sI https://nurazhar.com/ — timeout
check2: curl -sI http://nurazhar.com/ — 200 OK
check3: curl -sI https://nurazhar.gitlab.io/homepage/ — 308 redirect
check4: dig nurazhar.com A — resolves to GitLab Pages IP
check5: curl to nurazhardotcom.github.io/nurazhar.com/ — 301 redirect
check6: docker run alpine ... bb build — FileNotFoundException!
```

Check 6 was the smoking gun. Simulating the CI environment locally with Docker revealed the hardcoded path issue immediately.

## The Actual Fixes Applied

```d2
direction: right
Fix1: dev.clj: resume copy now resilient {
  shape: document
  style.stroke: "#2e7d32"
}
Fix2: GitLab Pages: verify domain in settings {
  shape: document
  style.stroke: "#2e7d32"
}

Fix1 -> Fix2: requires human in loop
```

**Fix 1** is code (already committed). **Fix 2** requires a trip to the GitLab project settings page — verify the domain and ensure the DNS TXT record exists.

## Verdict

Two bugs, both from the same category: **assuming the build environment matches the development environment**.

The hardcoded path fix is straightforward defensive programming. The HTTPS domain fix is a reminder that "automatic" SSL on GitLab Pages still requires a DNS verification step that's easy to miss.

The M2M protocol blog post with its 23 D2 diagrams? It'll be live once the CI/CD pipeline runs successfully — and once `https://nurazhar.com/` finishes its Let's Encrypt handshake.
