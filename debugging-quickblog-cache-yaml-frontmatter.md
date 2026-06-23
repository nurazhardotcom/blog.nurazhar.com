Title: Debugging a Missing Blog Post — Quickblog Cache & YAML Frontmatter
Date: 2026-06-20
Tags: quickblog, clojure, babashka, debugging, blogging, yaml
Description: How a missing blog post revealed a silent cache bug and a YAML format incompatibility that crashes quickblog's tag generation.

---

I published a new post. Pushed to `main`. Watched GitHub Actions deploy succeed with green checkmarks. But the URL returned 404.

## The Investigation Trail

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    A["Post published to main"]
    B["Deploy workflow ran"]
    subgraph C["gh-pages updated?"]
    end
    D["Check deploy logs"]
    E["Post should work"]
    F["Check which files deployed"]
    G["Only atom.xml + planetclojure.xml"]
    H["No HTML generated!"]
    B --> C
    C -->|"No"| D
    C -->|"Yes"| E
    D --> F
    F --> G
    G --> H
```

The deploy workflow ran fine — but the `gh-pages` branch only had two files changed: `atom.xml` and `planetclojure.xml`. No new HTML post. Worse, the workflow exit code was `0` (success), but `bb quickblog render` had silently skipped my post.

## Root Cause #1: Stale Cache

Quickblog caches post metadata in `.work/prod/cache.edn`. When I checked, the cache had 54 posts — all created before my new post existed.

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    A["Post added to posts/"]
    B["Quickblog scans posts/"]
    subgraph C["Cache exists?"]
    end
    D["Loads old post list"]
    E["Scans all posts fresh"]
    F["New post invisible"]
    G["Skipped during render"]
    H["New post included"]
    B --> C
    C -->|"Yes - stale"| D
    C -->|"No"| E
    D --> F
    F --> G
    E --> H
```

Clearing `.work/` forced a fresh scan. The post now rendered — but hit a second error:

```
Skipping post aur-audit-pgp-keys-wkhtmltopdf.md due to exception:
java.lang.IllegalArgumentException: Don't know how to create ISeq from: java.util.Date
```

## Root Cause #2: YAML Date Parsing

My post used YAML frontmatter:

```yaml
---
title: "How I Fixed a Broken AUR Install in 2 Commands"
date: 2026-06-19
tags: ["arch-linux", "aur", "pgp", "troubleshooting"]
---
```

The `---` delimiters trigger SnakeYAML parsing. SnakeYAML interprets `date: 2026-06-19` as a `java.util.Date` object — not a string.

## The Cascade Crash

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    A["YAML date: 2026-06-19"]
    B["SnakeYAML parses as java.util.Date"]
    C["sort-by :date crashes"]
    D["ClassCastException on Mixed Types"]
    E["post-by-tag fails"]
    F["Tag pages corrupted"]
    G["index.html not updated"]
    H["archive.html not updated"]
    I["Deploy succeeds but missing pages"]
    B --> C
    C --> D
    D --> E
    E --> F
    E --> G
    E --> H
    F --> I
```

Quoting the date (`date: "2026-06-19"`) seemed to fix it — but created a twist.

SnakeYAML then returned a `Long` instead of a `Date`. All other 54 posts produce **strings**. Mixing `Long` with `String` in `sort-by` causes:

```
ClassCastException: java.lang.String cannot be cast to java.lang.Character
```

## The Render Lifecycle

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    A["bb quickblog render"]
    B["Read post HTMLs"]
    C["Write individual posts"]
    subgraph D["write-post! x54"]
    end
    subgraph E["posts-by-tag - CRASH HERE"]
    end
    subgraph F["Write tag pages"]
    end
    subgraph G["Write index.html"]
    end
    subgraph H["Write archive.html"]
    end
    subgraph I["Write atom.xml"]
    end
    B --> C
    C --> D
    D --> E
    E --> F
    E --> G
    E --> H
    E --> I
```

The render wrote all 55 post HTMLs successfully — then crashed writing tag pages. I had raw HTML files but no `index.html`, no `archive.html`, no RSS feed.

## Format Comparison

| Format | Delimiters | Date Type | Tags Format | Used By |
|--------|------------|-----------|-------------|---------|
| Old-style | None | String | `Tag1, Tag2` | 54 of 55 posts |
| YAML-style | `---` | `Long`/`Date` | `["Tag1", "Tag2"]` | 1 post (mine) |

The format mismatch was invisible until the type check in `sort-by`.

## The Fix

Convert to quickblog-native format:

```text
Title: How I Fixed a Broken AUR Install in 2 Commands
Date: 2026-06-19
Tags: arch-linux, aur, pgp, troubleshooting
Description: Using aur-audit to diagnose missing PGP keys blocking wkhtmltopdf-bin installation on Arch.

---
```

No `---` delimiters. No YAML parsing. No type ambiguity.

## Manual Deploy Required

Since CI crashed, I deployed manually:

```bash
rm -rf .work
bb quickblog render  # Now generates index.html, tags/, everything
git worktree add /tmp/gh-pages origin/gh-pages
cd /tmp/gh-pages
git rm -rf . && cp -r /path/to/public/* .
git push --force origin HEAD:gh-pages
```

## The Fix Verification

| Step | Status |
|------|--------|
| Post HTML exists | ✅ 10KB file generated |
| Tag pages exist | ✅ `tags/pgp.html`, `tags/archlinux.html` |
| Index includes post | ✅ `grep -c aur-audit-pgp` in `index.html` |
| RSS updated | ✅ `atom.xml` contains entry |

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    A["Check HTML file"]
    subgraph B["Exists?"]
    end
    C["Verify tags"]
    D["Revert changes"]
    subgraph E["Tags generated?"]
    end
    F["Check index.html"]
    G["Fix format"]
    subgraph H["Post listed?"]
    end
    I["SUCCESS"]
    J["Check metadata"]
    B -->|"Yes"| C
    B -->|"No"| D
    C --> E
    E -->|"Yes"| F
    E -->|"No"| G
    F --> H
    H -->|"Yes"| I
    H -->|"No"| J
```

## Lessons Learned

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    subgraph root["root"]
        l1_1["root((War Story))"]
        l2_2["Cache"]
        l3_3[".work/ must be cleared on new posts"]
        l3_4["Cache.edn hides missing content silently"]
        l2_5["YAML"]
        l3_6["date: YYYY-MM-DD becomes java.util.Date"]
        l3_7["Breaks sort-by on mixed post types"]
        l2_8["Format"]
        l3_9["Old-style: Title:, Date:, Tags: (no delimiters)"]
        l3_10["YAML-style: title:, date:, tags: with ---"]
        l3_11["Cannot mix in same blog"]
        l2_12["Render Order"]
        l3_13["Individual posts render first"]
        l3_14["Tag/index crash doesn't revert"]
    end
```

1. **Clear `.work/` on new posts** — stale cache hides content
2. **YAML frontmatter ≠ quickblog format** — `---` delimiters change parsing
3. **SnakeYAML types differ** — `Date` and `Long` vs `String`
4. **CI can succeed partially** — exit 0 but missing critical files
5. **Always verify deploy output** — check `gh-pages` for actual changes

## Next Steps

Report the type-safety bug upstream. Quickblog should handle mixed frontmatter formats gracefully, or provide a clear error message instead of crashing mid-render.

---

*Running on: Babashka quickblog (git sha: c542bdd). CachyOS (Arch-based).*