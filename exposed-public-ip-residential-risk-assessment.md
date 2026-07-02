Title: Your Public IP Was in a Blog Post — Now What? A Practical Risk Assessment
Date: 2026-06-22
Tags: security, privacy, PDPA, networking, NAT, risk-assessment
Description: I accidentally published my home IP in a blog post. Here's the actual threat model — what's at risk, what isn't, and why a residential IP behind NAT is not the disaster it sounds like.

---

I published my home public IP in two blog posts. This is bad for PDPA compliance (it's personal data), but **how bad is it for actual security?**

Let's walk through the real threat model.

---

## What They See

Your public IP is not a secret. It's visible to:

- Every website you visit (logged in server access logs)
- Every CDN you hit (Cloudflare, Fastly, Akamai)
- Every service you connect to (DNS resolvers, NTP servers, API endpoints)
- BGP looking glasses, Shodan, Censys (which constantly scan the entire IPv4 space)
- Torrent swarms (if you use them)
- Anyone you send a direct message to on Discord/Telegram (peer-to-peer calls leak IPs)

Your IP being in a blog post is, for most practical attackers, *no more useful* than your IP already being in Shodan's database — which it already is if your ISP delegates a public IP.

```d2
# Diagram 78
direction: down

Shodan: "Shodan / Censys\nScans entire IPv4 space\nevery few hours" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
Blog: "Blog post\nwith your IP" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
Botnets: "Botnets\nConstant port scans" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
Attacker: "Attacker" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
Note: "For a residential IP, the blog post\nadds zero new attack surface.\nThe bots already found you." {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}

Shodan -> Attacker: "Already knows your IP"
Blog -> Attacker: "Same IP, no new info"
Botnets -> Attacker: "Already scanning it"
```

---

## The Real Attack Surface of a Home Connection

Here's what an attacker can do with your IP, ranked by likelihood:

### 1. Port scanning (already happening)

Every public IP is scanned by multiple botnets within hours of going online. They probe for:

- Open SSH (port 22) with weak passwords
- Telnet (port 23) — often left open on consumer routers
- RDP (port 3389) — Windows remote desktop
- UPnP (port 1900) — can expose internal services
- Common IoT vulns (Mirai-style)

**If you don't port forward, this is harmless.** The ONT's NAT firewall drops inbound SYN packets that don't match an established connection. The scan sees nothing.

### 2. Targeted attacks (requires vulnerability)

Having your IP doesn't help an attacker compromise your devices. They need:

- A known vulnerability in your router firmware
- Forwarded ports exposing an internal service
- Weak credentials on a publicly accessible interface

**If you keep firmware updated, don't port-forward, and don't expose admin panels to WAN, there's no viable attack path.**

### 3. DDoS (annoying, not dangerous)

Someone can direct a DDoS at your IP. This will:

- Saturate your Singtel connection (500 Mbps down / 250 Mbps up for a typical Fibre plan)
- Cause packet loss and high latency
- Not compromise any device

If this happens consistently, you can request a new IP from your ISP or use a DDoS-protected VPN tunnel.

### 4. Geolocation / doxxing (privacy, not security)

An IP reveals approximate location (usually the ISP's PoP, not your street address). For Singtel Fibre in Singapore, this typically resolves to:

```
City: Singapore
ISP: Singtel Fibre (AS9506)
Organization: Singapore Telecom
```

That's neighbourhood-level at best. Not your home address.

---

## When an Exposed IP IS Dangerous

The risk profile changes if you:

- **Run a server at home** (web, game, SSH) with forwarded ports
- **Host services on non-standard ports** that are discoverable via scans
- **Have a static IP** (means the IP won't change, giving attackers a permanent target)
- **Use default credentials** on your router admin interface exposed to WAN
- **Have vulnerable IoT devices** on UPnP-accessible ports

None of these apply to a typical residential connection behind NAT with no forwarded ports.

---

## What I Actually Did

1. **Redacted the IP from all blog posts** — This was the right call. Not because the IP is sensitive, but because it's personal data under PDPA. Publishing someone's IP without consent (even your own, in a way that identifies you) is a compliance risk.

2. **Restarted the ONT to get a new IP** — Didn't work (sticky DHCP lease). Not worth pursuing — the IP was already redacted from the public record on my site.

3. **Wrote this post** — To clarify that the *security* risk of an exposed residential IP is near-zero, even though the *compliance* risk of publishing personal data is real.

---

## The Bottom Line

```
Risk of publishing your residential IP:
  PDPA compliance: ⚠️ Medium (do not publish personal data)
  Security impact:  ✅ Near-zero (behind NAT, no open ports)
  DDoS risk:        ✅ Low (annoying but harmless)
  Doxxing risk:     ⚠️ Minimal (neighbourhood-level geolocation)
```

The fix for a leaked IP is simple: redact it from wherever you published it. That's sufficient. The ONT restart is optional — your IP will change eventually on its own, and even if it doesn't, no new attack surface was created.

---

*This post is not legal advice. PDPA enforcement varies by jurisdiction and context. If you're unsure, consult a lawyer. The security assessment applies to typical residential NAT connections — if you run public-facing services, your threat model is different.*
