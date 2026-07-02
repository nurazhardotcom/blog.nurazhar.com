Title: The 4-Desktop Cognitive Firewall — KWin Rules That Killed My Context Switch Loop
Date: 2026-07-02
Tags: cachyos, kde, kwin, wayland, productivity, cognitive-load, focus, architecture, intj, fish, konsole, workspace
Description: Waiting on builds is when the damage happens. A reflexive browser tab switch breaks the sequential focus loop. Here is the KDE KWin rule configuration that locks the distraction out.

---

![4-Desktop Cognitive Firewall](cognitive-workspace-firewall.svg)

**The problem:** I am waiting 30-180 seconds on `paru -S`, `bb build`, or a Clojure compile. The muscle memory reflex is `Alt+Tab` → browser → graze. That reflex breaks a sequential focus loop that costs 15+ minutes to rebuild.

**The fix:** Four virtual desktops with hard KWin bindings. No app crosses its assigned desktop. No browser on the DEV desktop. No terminal on COM. The panel Pager shows desktop names, not window previews — removing the visual trigger to peek.

## The Architecture

| Desktop | Name | Apps Allowed | KWin Rule |
|---------|------|-------------|-----------|
| 1 | DEV | Konsole, Neovim, fish | Force desktop, fixed |
| 2 | WEB | Firefox (docs, localhost) | Force desktop, fixed |
| 3 | OPS | btop, journalctl, deploy logs | Force desktop, fixed |
| 4 | COM | Slack, Signal, email | Force desktop, fixed, muted |

## The KWin Rules (Step by Step)

1. Open **System Settings** → **Window Management** → **KWin Rules**
2. For each application, create a new rule:
   - **Window matching**: `Window class` contains `<app>` (e.g., `org.kde.konsole`)
   - **Desktops**: Set to `Force` → `Exactly` → `Desktop 1`
   - **Desktops (Fixed)**: Set to **Force** → **Yes** — prevents any `Alt+Tab` or click from pulling it elsewhere
3. Enable **Desktops (Fixed)** on every rule. This is the non-negotiable flag. Without it, clicking a browser link from within Konsole drags Firefox onto Desktop 1 — firewall breached.

For the Pager:
- Right-click the **Icons-only Task Manager** → **Configure** → uncheck `Show tooltips` and `Show window previews on hover`
- Add a **Pager** widget to the panel → **Configure Pager** → set `Display` → `Desktop name` (not previews)

## The Behavioral Lock

The panel shows four names: **DEV WEB OPS COM**. No window thumbnails. When I wait on a build, I see the DEV label and my brain stays in context.

The Pager is a display-only UI — clicking it requires `Ctrl+F1-F4` or `Meta+1-4` anyway. The friction of switching desktops is intentional. It forces the question: *Do I actually need to switch?*

## The Config Dump

No screenshots. Here are the `kwinrulesrc` entries for Konsole:

```
[Rule for Konsole]
Description=Konsole -> DEV
windowclass=org.kde.konsole
windowtypes=1
desktops=1
desktopsrule=2
desktopfixedrule=2
```

`rule=2` means `Force`. `desktops=1` is Desktop 1. `desktopfixedrule=2` is the breaker — `desktopfixed=true, forced`.

## The Cost

Four minutes to configure across 6 applications. Eliminated a context-switch tax I was paying 5-10 times per hour. ROI cleared in half a day.

## The Takeaway

The browser is not the problem. The reflexive desktop arrival during an idle loop is the problem. KWin rules are the surgical fix — no habit change required, just physical architecture. The config above is copy-pasteable.
