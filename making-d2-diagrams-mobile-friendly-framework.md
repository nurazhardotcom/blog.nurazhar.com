Title: Making D2 Diagrams Mobile-Friendly — A Static Site Framework
Date: 2026-06-22
Tags: meta, d2, mobile, ux, static-site, css, engineering, automation
Description: How we built an automated framework for D2 diagrams on a static blog — auto-fixing v0.6.9 quirks at compile time, scaling SVGs on mobile, and replacing simple diagrams with responsive HTML cards.

---

This blog uses [D2](https://d2lang.com/) for diagrams — architecture flows, framework overviews, comparison matrices. It's a great tool for complex layouts, but we hit three problems:

1. **D2 v0.6.9 treats `$` as a substitution prefix** — diagrams with `$200/session` or `~$0.50/hr` fail to compile with "substitutions must begin on {"
2. **D2's layout engine places nodes side-by-side** even with `direction: down`, producing 700px+ wide SVGs that scale to ~7px text on a 375px phone
3. **Every post needed manual fixes** — escaping `$`, rewriting `\n`, checking mobile layouts

The fix is a three-tier framework that decides how to render each diagram based on its complexity.

---

## Tier 1: Simple Relationships → HTML Cards

For diagrams with 2-4 nodes and no meaningful connections (just a relationship), D2 is overkill. The SVG scales down to unreadable text on mobile. Instead, we render these as inline HTML cards:

<div style="display: flex; flex-direction: column; gap: 8px; max-width: 380px; margin: 24px auto; font-size: 14px; line-height: 1.5;">

<div style="border: 2px solid #0284c7; background: #e0f2fe; border-radius: 12px; padding: 14px; text-align: center;">
  <strong style="font-size: 15px;">Human Coach</strong><br>
  <span style="font-size: 12px; color: #475569;">Accountability · Emotional support</span>
</div>

<div style="text-align: center; font-size: 20px; color: #64748b;">↓</div>

<div style="border: 2px solid #0ea5e9; background: #f0f9ff; border-radius: 12px; padding: 14px; text-align: center;">
  <strong style="font-size: 15px;">AI Coach (LLM)</strong><br>
  <span style="font-size: 12px; color: #475569;">Data-driven analysis · 24/7 availability</span>
</div>

<div style="text-align: center; font-size: 20px; color: #64748b;">↓</div>

<div style="border: 2px dashed #10b981; background: #d5f5e3; border-radius: 12px; padding: 14px; text-align: center;">
  <strong style="font-size: 15px;">Best Together</strong><br>
  <span style="font-size: 12px; color: #475569;">AI does the audit. Human does the grounding.</span>
</div>

</div>

Cards naturally stack vertically, the text stays readable at any width, and there's zero dependency on SVG rendering. We converted 5 posts to this pattern.

---

## Tier 2: Complex Diagrams → D2 + CSS Scaling

For architecture diagrams, framework overviews, and process flows with 5+ nodes and labeled connections, D2's layout engine is the right tool. We keep these as compiled SVGs but fixed the CSS to make them scale:

```css
/* Before: diagrams overflowed on mobile */
@media (max-width: 768px) {
  .d2-diagram svg {
    max-width: none;  /* ← bug: full width, user must scroll */
  }
}

/* After: diagrams scale to fit */
.d2-diagram svg {
  max-width: 100%;
  height: auto;
}
```

With `preserveAspectRatio="xMinYMin meet"` and `viewBox` on the D2 output, the SVG now scales down proportionally. Complex diagrams become smaller but remain readable — users can pinch-zoom for detail.

---

## Tier 3: Data Comparisons → Markdown Tables

For dimension-by-dimension comparisons, D2 containers with many sub-items force a wide grid layout. A markdown table scrolls horizontally on mobile and keeps everything readable:

| Dimension | Human Coach | AI Coach |
|-----------|-------------|----------|
| Cost | $200-500/session | ~$0.50/hr API |
| Context | ~50 min memory | 1M token context |
| Availability | Weekly sessions | 24/7 |

The CSS already handles `overflow-x: auto` on tables, so no extra work needed.

---

## The Auto-Fix Layer

The trickiest part was D2 v0.6.9's `$` substitution. Every post with money amounts (`$200`, `$0.50`) would fail at compile time. Instead of manually escaping every post, we added a preprocessing step in `preprocess_d2.py` that auto-fixes D2 code before passing it to the compiler:

```python
def fix_d2_for_v069(d2_code):
    # Escape $ → \$ (d2 v0.6.9 treats $ as substitution prefix)
    d2_code = re.sub(r'(?<!\\)\$', r'\\$', d2_code)
    # Join multi-line strings: lines inside "..." get \n-joined
    # ...
    return d2_code
```

This runs at compile time — source files stay clean with natural `$` signs. We also added a validator that scans for bare `direction:` directives outside code fences, catching corrupted D2 blocks before they reach production:

```
WARNING: broken D2 blocks detected (will show raw source in HTML):
  ⚠ filename.md:42: bare 'direction:' directive (missing ```d2 fence?)
```

---

## The Decision Framework

| Diagram type | Tool | Mobile behavior | Example |
|-------------|------|----------------|---------|
| Simple relationship (2-4 nodes) | HTML cards | Vertical stack, readable at any width | AI vs Human Coach comparison |
| Complex diagram (5+ nodes, connections) | D2 SVG | Scales with `max-width: 100%`, pinch-zoom | Scrum Framework, architecture flows |
| Data comparison | Markdown table | Horizontal scroll via `overflow-x: auto` | Feature comparison matrices |

The framework is documented in `AGENTS.md` so any AI agent working on the blog follows the same rules automatically. No more manual fixes, no more broken diagrams, no more unreadable mobile layouts.
