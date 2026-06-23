Title: Dumping Flatpak from CachyOS — 5.8GB Reclaimed
Date: 2026-06-20
Tags: cachyos, flatpak, packages, cleanup, kde, gnome
Description: Removing Flatpak and its massive runtime bloat reclaimed nearly 6GB on my KDE CachyOS system

---

CachyOS ships clean. But I managed to accumulate **5.8GB of Flatpak overhead** without really thinking about it. Here's what happened and how I fixed it.

## The Problem

My system showed:
```
Packages: 7 (flatpak-user), 1413 (pacman)
Disk: 33% used (163/472 GB)
```

Seven Flatpak packages seemed harmless. Then I checked the actual disk usage:

```bash
flatpak list --user -d --columns=application,install-size
# Showed:
# Pieces OS              → 3.9 GB
# GNOME Platform 48        → 1.1 GB + EOL warning
# Mesa GL runtime (2x)     → 914 MB  
# Other runtimes           → ~15 MB
# -----------------------------------
# Total                    → 5.8 GB
```

Flatpak was honest about the **package count** but silent on **runtime bloat**. 3.9GB for an on-device AI engine alone.

## The Gotcha — PAM Faillock Lockout

Attempting to remove the `flatpak` package triggered sudo failures:

```bash
faillock --user nurazhar
# When    Type  Source  Valid
# 10:35   SVC   sudo    V
# 10:35   SVC   sudo    V
```

"V" = valid failure entries. Arch's `pam_faillock.so` locks accounts after repeated failed sudo attempts. The flatpak removal had failed silently in my script, counting as failures.

**Fix:** Switch to TTY (Ctrl+Alt+F2), login as root, reset:

```bash
faillock --user nurazhar --reset
```

Switch back (Ctrl+Alt+F1) and sudo works.

## The Cleanup

```bash
# Remove user flatpak apps + runtimes
flatpak uninstall -u --delete-data -y com.pieces.pfd com.pieces.os \
    org.gnome.Platform org.gnome.Platform.Locale \
    org.freedesktop.Platform.GL.default \
    org.freedesktop.Platform.openh264 \
    org.gtk.Gtk3theme.Breeze

# Delete directories
rm -rf ~/.local/share/flatpak ~/.var/app

# Remove system package
sudo pacman -Rns flatpak
```

Total removed: **5.8GB user data + 13MB system packages**.

## Lessons Learned

1. **Flatpak hides true disk cost** — package count ≠ storage impact
2. **GNOME runtimes on KDE are wasteful** — mismatched DE doubles footprint
3. **EOL runtimes are security risks** — GNOME 48 expired March 2024
4. **PAM faillock is aggressive** — silent sudo failures lock you out
5. **TTY fallback always works** — root login bypasses user lockout

## Tradeoffs

| Aspect | Before | After |
|--------|--------|-------|
| Disk space | 5.8GB overhead | Clean |
| Pieces access | Desktop app | Web app/VS Code extension |
| Runtime security | GNOME 48 EOL | N/A |
| Sandboxing | Flatpak isolation | None |

## Quick Reference

```bash
# Check flatpak size
flatpak list --user -d --columns=application,install-size

# Check for lockout
faillock --user $USER

# Reset lockout (root/tty only)
faillock --user $USER --reset

# Remove flatpak + deps
sudo pacman -Rns flatpak
```

For me, worth it. I use Pieces primarily through VS Code extension anyway. 5.8GB is significant on a 163GB root partition. Plus, running an EOL runtime on a system I trust for security work? No thanks.

**Resume Value?** Yes — demonstrates:
- Linux package management (pacman, flatpak)
- PAM authentication troubleshooting
- Disk hygiene for constrained systems
- Tradeoff analysis (security vs convenience)

## Links

- [Pieces web app](https://pieces.app) — browser-based alternative
- [Arch PAM faillock docs](https://wiki.archlinux.org/title/PAM) — authentication troubleshooting