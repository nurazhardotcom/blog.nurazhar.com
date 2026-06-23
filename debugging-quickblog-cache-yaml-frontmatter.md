Title: Debugging a Missing Blog Post — Quickblog Cache & YAML Frontmatter
Date: 2026-06-20
Tags: quickblog, clojure, babashka, debugging, blogging, yaml
Description: How a missing blog post revealed a silent cache bug and a YAML format incompatibility that crashes quickblog's tag generation.

---

I published a new post. Pushed to `main`. Watched GitHub Actions deploy succeed with green checkmarks. But the URL returned 404.

## The Investigation Trail

```d2
# Diagram 68
direction: down

A: "Post published to main" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
B: "Deploy workflow ran" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
C: "gh-pages updated?" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}
D: "Check deploy logs" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
E: "Post should work" {
  style.fill: "#d4edda"
  style.stroke: "#c3e6cb"
}
F: "Check which files deployed" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
G: "Only atom.xml + planetclojure.xml" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}
H: "No HTML generated!" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}

B -> C
C -> D: "No"
C -> E: "Yes"
D -> F
F -> G
G -> H
```

The deploy workflow ran fine — but the `gh-pages` branch only had two files changed: `atom.xml` and `planetclojure.xml`. No new HTML post. Worse, the workflow exit code was `0` (success), but `bb quickblog render` had silently skipped my post.

## Root Cause #1: Stale Cache

Quickblog caches post metadata in `.work/prod/cache.edn`. When I checked, the cache had 54 posts — all created before my new post existed.

```d2
# Diagram 69
direction: down

A: "Post added to posts/" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
B: "Quickblog scans posts/" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
C: "Cache exists?" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}
D: "Loads old post list" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
E: "Scans all posts fresh" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
F: "New post invisible" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
G: "Skipped during render" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
H: "New post included" {
  style.fill: "#d4edda"
  style.stroke: "#c3e6cb"
}

B -> C
C -> D: "Yes - stale"
C -> E: "No"
D -> F
F -> G
E -> H
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

```d2
# Diagram 70
direction: down

A: "YAML date: 2026-06-19" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
B: "SnakeYAML parses as java.util.Date" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
C: "sort-by :date crashes" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
D: "ClassCastException on Mixed Types" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
E: "post-by-tag fails" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
F: "Tag pages corrupted" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
G: "index.html not updated" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
H: "archive.html not updated" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
I: "Deploy succeeds but missing pages" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}

B -> C
C -> D
D -> E
E -> F
E -> G
E -> H
F -> I
```

Quoting the date (`date: "2026-06-19"`) seemed to fix it — but created a twist.

SnakeYAML then returned a `Long` instead of a `Date`. All other 54 posts produce **strings**. Mixing `Long` with `String` in `sort-by` causes:

```
ClassCastException: java.lang.String cannot be cast to java.lang.Character
```

## The Render Lifecycle

```d2
# Diagram 71
direction: down

A: "bb quickblog render" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
B: "Read post HTMLs" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
C: "Write individual posts" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
D: "write-post! x54" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
E: "posts-by-tag - CRASH HERE" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
F: "Write tag pages" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
G: "Write index.html" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
H: "Write archive.html" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
I: "Write atom.xml" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}

B -> C
C -> D
D -> E
E -> F
E -> G
E -> H
E -> I
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

```d2
# Diagram 72
direction: down

A: "Check HTML file" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
B: "Exists?" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}
C: "Verify tags" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
D: "Revert changes" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
}
E: "Tags generated?" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}
F: "Check index.html" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}
G: "Fix format" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}
H: "Post listed?" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}
I: "SUCCESS" {
  style.fill: "#d4edda"
  style.stroke: "#c3e6cb"
}
J: "Check metadata" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
}

B -> C: "Yes"
B -> D: "No"
C -> E
E -> F: "Yes"
E -> G: "No"
F -> H
H -> I: "Yes"
H -> J: "No"
```

## Lessons Learned

```d2
# Diagram 73
direction: down

root: "root" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"

  l1_1: "War Story" {
    style.fill: "#e8f4fd"
    style.stroke: "#bbeeeb"
  }
  l2_2: "Cache" {
    style.fill: "#fff3cd"
    style.stroke: "#ffeeba"
    l3_3: ".work/ must be cleared on new posts" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
    l3_4: "Cache.edn hides missing content silently" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
  }
  l2_5: "YAML" {
    style.fill: "#fff3cd"
    style.stroke: "#ffeeba"
    l3_6: "date: YYYY-MM-DD becomes java.util.Date" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
    l3_7: "Breaks sort-by on mixed post types" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
  }
  l2_8: "Format" {
    style.fill: "#fff3cd"
    style.stroke: "#ffeeba"
    l3_9: "Old-style: Title:, Date:, Tags: (no delimiters)" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
    l3_10: "YAML-style: title:, date:, tags: with ---" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
    l3_11: "Cannot mix in same blog" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
  }
  l2_12: "Render Order" {
    style.fill: "#fff3cd"
    style.stroke: "#ffeeba"
    l3_13: "Individual posts render first" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
    l3_14: "Tag/index crash doesn't revert" {
      style.fill: "#ffffff"
      style.stroke: "#ffeeba"
    }
  }
}
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