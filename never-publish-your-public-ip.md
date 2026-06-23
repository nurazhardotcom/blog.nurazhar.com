Title: I Published My Public IP on the Internet — Here's What I Did Wrong
Date: 2026-06-22
Tags: security, privacy, networking, pdpa, ops, singtel
Description: How I accidentally hardcoded my public IP in two blog posts, the security implications, and why restarting my router was the real fix — not rewriting git history.

---

I write technical blog posts. As part of that, I traced my machine's network path — GPON ONT, BRAS, DNS servers, submarine cables — and published the raw data. Including my public IP.

It was a copy-paste error. I didn't think about it because the IP was just a technical datapoint in a packet trace. But in Singapore, under PDPA, your public IP is **personal data** — it can identify you, your rough location (suburb level via WHOIS/GeoLite), your ISP, and can be correlated with your browsing history by your ISP or any intermediary.

Here's what I did to fix it, and why git history rewriting was overkill.

## The Problem

My public IP `116.15.188.x` appeared in two posts:

1. **Where Your Prompt Goes** — packet trace from Singtel Fibre, showing source IP in the path description and D2 network diagrams
2. **Why I Switched to Cloudflare DNS** — DNS benchmark output showing the IP as context

Both were published, indexed, and live.

## Step 1: Check Current Exposure

Immediately checked if the live pages still had the IP:

```bash
# Check what GitHub Pages is serving
curl -s https://blog.nurazhar.com/where-your-packet-goes-submarine-cables/ | grep -i "116.15"
curl -s https://blog.nurazhar.com/switching-to-cloudflare-dns/ | grep -i "116.15"
```

Github Pages deployments are declarative — whatever is in `gh-pages` branch is the published version. So I:

1. Edited the Markdown files to replace the IP with `[redacted]`
2. Committed and pushed to `main`
3. CI rebuilt and deployed to `gh-pages`

Live site was clean.

## Step 2: Does Git History Matter?

The IP was now gone from the current revision. But the commit history still contained it. Anyone could `git log -p` and see the old IP.

This is where I over-engineered. I ran `git filter-branch` to rewrite history — essentially creating a parallel universe where the IP never existed. But:

- **GitHub already has the old history** — you'd need `git push --force --all --tags` and every collaborator to rebase
- **The IP is dynamic** — Singtel Fibre uses DHCP. Restarting the ONT assigns a new IP. The old one goes back into the ISP pool and gets reissued to someone else
- **Anyone who already cloned the repo has the IP** — you can't unsend a download

## Step 3: The Real Fix — Restart the Router

```bash
# No tool needed. Just unplug the Singtel ONT for 30 seconds
# Plug back in. New IP.
```

My Singtel Fibre ONT (ZTE F660, `192.168.1.254`) uses DHCP with a lease time of 24 hours typically. Power-cycling forces a new lease. New IP — the old one becomes someone else's problem.

This is the cheap, pragmatic security move. It's why you shouldn't treat residential IPs as persistent identifiers.

## Lessons

1. **Don't hardcode IPs in published content** — use `[redacted]` from the start, or use environment variables in build scripts to inject/redact them
2. **Git history rewriting is usually the wrong answer for dynamic IPs** — the IP loses value as soon as the device disconnects
3. **PDPA applies to blog posts** — a public IP can identify you, especially when combined with neighbourhood-level geolocation, ISP, and timestamps
4. **If you do need to remove something from git history** for persistent secrets (private keys, passwords), use `git filter-repo` or `git filter-branch` — but only for secrets that don't expire. Dynamic IPs expire on their own.
5. **Router restart is a valid security control** — it's cheap, immediate, and terminates any lingering state tied to the old IP

## Timeline

- **09:00** — Published packet trace post with hardcoded IP
- **09:15** — Published DNS post with same IP
- **12:30** — User (me) noticed and flagged it in the coding assistant session
- **12:31** — IP redacted in source files, committed, pushed, deployed
- **12:35** — `git filter-branch` experiment (reverted — unnecessary for dynamic IPs)
- **12:40** — Decision: restart ONT, don't force-push rewritten history

The actual security fix took one minute. The over-engineering took ten.

## Key Takeaway

> A public IP is only a secret until your router reboots.

Act fast, redact the content, restart the connection, and move on. Don't rewrite history for something that expires.
