Title: Setting Quad9 Private DNS on Android
Date: 2026-06-22
Tags: android, dns, quad9, privacy, doh
Description: How to set Quad9 as your private DNS provider on Android — encrypted DNS without root or apps.

---

Android 9+ has a built-in **Private DNS** mode (DNS-over-TLS). No app, no root, no VPN config needed.

## Steps

1. Open **Settings** → **Connections** → **More connection settings** → **Private DNS**
2. Select **Private DNS provider hostname**
3. Enter:

```
dns.quad9.net
```

4. Tap **Save**

## Verification

Open a browser and visit `https://dnsleaktest.com` — it should show Quad9 (`9.9.9.9`) as your resolver, not your ISP.

## Why Quad9

- **Swiss jurisdiction** — not subject to US CLOUD Act
- **Zero-logging** — independently audited
- **DoT encrypted** — your mobile carrier can't see which domains you visit
- **Blocks known malware** domains by default

That's it. One field, 10 seconds, encrypted DNS on all Wi-Fi and mobile data from your phone.

---

*Tested on Android 14 / One UI 6.1. Works on stock Android 9+ and all OEM skins.*
