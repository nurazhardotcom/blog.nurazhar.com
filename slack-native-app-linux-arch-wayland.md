Title: Slack Native App on Linux — The "Beta" Label Is a Lie, Here Is What Arch Users Actually Run
Date: 2026-07-02
Tags: slack, linux, arch, cachyos, wayland, kde, electron, flatpak, aur
Description: Slack's Linux client has worn a "beta" badge since 2016. It is production software used by millions. Here is the actual install command for CachyOS/KDE/Wayland and what Arch users run instead of the browser.

---

![Slack on Arch Linux — Client options workflow](slack-arch-options.svg)

I needed Slack on CachyOS with KDE Plasma 6 on Wayland. The download page screams **beta** and points to `.deb`/`.rpm`. Arch users do not use those. We use the AUR — and the "beta" label is purely vestigial marketing.

## The Options

| Option | Command | Wayland Status |
|--------|---------|----------------|
| `slack-desktop-wayland` (AUR) | `paru -S slack-desktop-wayland` | Patched for Wayland. Screen sharing works via PipeWire |
| Flatpak `com.slack.Slack` | `flatpak install flathub com.slack.Slack` | Portal-based Wayland. Sandboxed, auto-updates |
| `slack-desktop` (AUR) | `paru -S slack-desktop` | Works. May need `--gtk-version=3` on KDE |
| `slack-term` (AUR) | `paru -S slack-term` | Terminal TUI. Lightweight. 6.6k ⭐ on GitHub |
| **Ripcord** (AUR) | `paru -S ripcord` | Native Qt. **Paid ~$30** for Slack (Discord is free) |

## What I Installed

```bash
paru -S slack-desktop-wayland
```

CachyOS ships **paru** (Rust AUR helper, faster than yay). The `wayland` variant is the official Slack `.deb` repackaged with two flags: `--ozone-platform=wayland` and `--enable-features=WebRTCPipeWireCapturer`. No config files, no env vars — launches and screen sharing just works on KDE/Wayland.

## The "Beta" Label Is Meaningless

- **Version 4.50.x** — current, updated monthly alongside Windows/Mac
- **Same Electron codebase** — zero feature gaps (huddles, screen share, threads, all work)
- **Used in production** at companies of every size since 2016

Slack never removed the badge because Linux packaging is not their priority. The AUR tracks upstream releases within hours. This is not a beta — it is the same software they ship to everyone else.

## What Arch Users Actually Run

Most run `slack-desktop` or `slack-desktop-wayland` from AUR. Some prefer Flatpak for sandboxing. Terminal users reach for `slack-term`. **Ripcord** fans exist but the paid Slack license turns most people off.

## The Takeaway

The "beta" banner is a lie. Run `paru -S slack-desktop-wayland` on CachyOS/KDE/Wayland and get a working native app with screen share in 30 seconds.