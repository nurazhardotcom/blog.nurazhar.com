Title: 5.2 MB to 169 KB — Performance Auditing a Static Blog
Date: 2026-06-22
Tags: performance, css, web, optimization, blogging, static-site
Description: My blog's homepage was 5.2 MB. The fix was a 5-line template change. Here's the audit, the fix, and the general lesson about static site performance.

---

I checked my blog's homepage recently:

```
$ curl -s -o /dev/null -w "Size: %{size_download} bytes | TTFB: %{time_starttransfer}s\n" blog.nurazhar.com
Size: 5,255,649 bytes | TTFB: 0.718s
```

**5.2 MB for a text blog.**

Individual post pages were fine (~41 KB, ~0.29s). But the homepage was inlining the full body content of all 107 posts — including all D2 SVG diagrams, comparison tables, and code blocks.

---

## The Root Cause

The index template was rendering `{{post.body | safe}}` for every post. For 107 posts with D2 diagrams, this ballooned to 5.2 MB.

The fix was trivial — show only metadata on the index:

```
Title + Description + Date + Tags
```

No body content. No SVGs. No code blocks.

Result:

```
Size: 168,695 bytes | TTFB: 0.187s
```

**97% reduction.** From 5.2 MB to 169 KB. TTFB dropped from 718ms to 187ms.

---

## The Scale Question

My config has `:num-index-posts 365`. I initially worried this would be a bottleneck at scale. It's not:

| Format | Size per post | 107 posts | 1,000 posts |
|--------|--------------|-----------|-------------|
| Full body (old) | ~49 KB avg | 5.2 MB | ~49 MB |
| Title + desc + tags (new) | ~200 bytes | 21 KB | ~200 KB |

Even at 1,000 posts, the index page is ~200 KB — smaller than a single post with one D2 diagram. The bottleneck was never the number of posts; it was inlining the content. Metadata scales linearly with negligible cost.

---

## Other Fixes

### Code Block Readability

The blog had syntax highlighting colors for dark mode only. Light mode users saw plain black-on-white code. Added a full light mode token color scheme — keywords in purple, strings in green, functions in amber, comments in muted gray.

### Render-Blocking Scripts

Prism.js syntax highlighter and its CSS were loaded synchronously in `<head>`, blocking first paint. Changed to:

- Prism.js: `defer` (loads after HTML parsing)
- Prism CSS: `preload` with async swap-in (prevents render block)

### Mobile

- Code blocks and tables: horizontal scroll on overflow
- Navigation: centered and wraps on small screens
- Padding: reduced from 24px to 16px on mobile
- D2 SVGs: scroll at render width so text stays readable

---

## The Lesson

For a blog, the index page should be a **catalogue**, not a firehose. Title, date, description, tags. That's it. The full content belongs on individual post pages, where they're cached and served independently.

The highest-impact optimization on this blog was a template change — bigger than any CDN, caching strategy, or script deferral. Sometimes the best performance fix is showing less data.
