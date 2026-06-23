Title: Building a Local Mermaid Viewer With the LLM That Can't Draw
Date: 2026-06-16
Tags: ai, tooling, mermaid, privacy, developer-experience, agentic-ide
Description: Yesterday I wrote about why LLMs repeatedly fail at Mermaid syntax. Today I used one to build a zero-dependency, offline-first Mermaid viewer — and it broke the diagram three times before getting it right.

---

Yesterday, I published [The Mermaid Loop: Why LLMs Repeatedly Fail at Diagram Syntax](https://blog.nurazhar.com/why-llms-fail-at-mermaid-diagrams.html). In that post I dissected *why* LLMs consistently generate broken Mermaid syntax — the unquoted punctuation trap, tokenization drift, and the lack of an inline compiler feedback loop.

Today, I used one of those LLMs to build the fix.

---

## The Problem: Viewing Mermaid Diagrams Without Getting Tracked

Every time I need to validate a Mermaid diagram, the default workflow is:

1. Copy the code block from my editor.
2. Open [mermaid.live](https://mermaid.live) or [mermaid-drawing.com](https://mermaid-drawing.com).
3. Paste my diagram — which often contains internal system architecture, pipeline names, staffing strategies, and client mappings.
4. Squint at the rendered output.
5. Close the tab and hope no telemetry captured my proprietary flowchart.

This is an unacceptable trade-off. I am sending sensitive operational diagrams to third-party hosted JavaScript applications with unknown data retention policies. Every paste is a potential leak.

The solution should be obvious: **run Mermaid.js locally**.

---

## The Build: One HTML File, Zero Dependencies

The spec was simple:

- **Single HTML file** — no npm, no build step, no server.
- **Runs from `file://`** — double-click to open, works in any browser.
- **Full zoom and pan** — mouse-wheel zoom toward cursor, click-drag pan, fit-to-view button.
- **Auto-strip markdown fences** — paste ```` ```mermaid ```` blocks directly, the viewer strips the fences before rendering.
- **Auto-save to localStorage** — your diagram persists across browser sessions.
- **SVG and PNG export** — download the rendered diagram without touching any external service.
- **Dark theme** — because obviously.

The entire viewer is **under 400 lines** of HTML, CSS, and vanilla JavaScript.

---

## The Irony: The LLM Broke Its Own Diagram (Again)

Here is the part that directly continues yesterday's post.

I asked the agentic IDE (Antigravity, running on Claude) to build the viewer and embed my career-ops pipeline diagram as a sample. The diagram contains subgraphs with parentheses, ampersands, and dollar signs:

```text
subgraph Jack ["Jack (jackandjill.ai) - High-Ground Strategist"]
    J_Narrative["Narrative Positioning & Calibrations (Janitor, BSV, Air-Gapped high-security)"]
end
```

The LLM generated the viewer — and the diagram inside it was broken. Three times.

### Failure 1: Template Literal Entity Mangling

The first version embedded the sample diagram inside a JavaScript template literal (backtick string). The `&` characters were silently converted to `&amp;` by the HTML parser before the JavaScript engine saw them. Mermaid received `&amp;` as literal text and choked.

**Root cause:** HTML entity parsing happens before JavaScript execution. The `&` in a `<script>` block's template literal is parsed by the HTML tokenizer first.

### Failure 2: Module Script Scope Isolation

The second version used `<script type="module">` to load mermaid dynamically. The button handlers were defined inside the module scope but referenced via inline `onclick=""` attributes in the HTML — which execute in global scope. Every button was dead on arrival.

**Root cause:** ES modules create their own scope. Inline `onclick` handlers cannot reach module-scoped functions.

### Failure 3: Race Condition on Dynamic Load

Still in version 2, the mermaid library was loaded via a dynamically created `<script>` element with an `onload` callback that called `initApp()`. But the rendering function was invoked before `mermaid.initialize()` had completed its internal async setup.

**Root cause:** `mermaid.initialize()` is synchronous, but the library's internal parser setup has deferred initialization that resolves on the next microtask tick.

### The Working Version (v3)

The fix required reverting to fundamentals:

1. **Load mermaid as a plain `<script src="...">` tag** — synchronous, blocking, no race.
2. **Wrap all JS in an IIFE** — no module scope, no global pollution.
3. **Wire all buttons with `addEventListener()`** — no inline handlers.
4. **Build the sample diagram with `[].join('\n')`** — no template literals, no entity mangling.
5. **Auto-strip markdown fences** — regex removes ```` ```mermaid ```` and ```` ``` ```` wrappers before handing code to the parser.

---

## The Meta-Lesson

Yesterday's post argued that LLMs fail at Mermaid because they lack an inline compiler feedback loop. Today's build session proved it empirically:

- **Iteration 1:** Broken by HTML entity rules the LLM didn't model.
- **Iteration 2:** Broken by JavaScript module scoping rules the LLM didn't verify.
- **Iteration 3:** Broken by async initialization timing the LLM didn't test.

Each fix only happened because *I* ran the code, observed the failure, and fed the error back. The LLM never once said "wait, let me check if this actually works." It confidently generated three broken versions in a row — each broken for a different reason.

This is the current state of agentic coding: **the human is the compiler**. Until AI IDEs integrate local runtime verification for structured outputs (Mermaid, SVG, CSS animations), the human developer remains the essential feedback loop.

---

## How to Use It

The viewer is a single HTML file. Save it anywhere and open it in your browser:

```bash
# Open directly
xdg-open ~/mermaid_viewer.html

# Or on macOS
open ~/mermaid_viewer.html
```

### Controls

| Action | Input |
|:---|:---|
| **Zoom in/out** | Mouse scroll wheel (zooms toward cursor) |
| **Pan** | Click and drag |
| **Fit to view** | Click ↗ toolbar button, or press `F` |
| **Reset to 100%** | Click □ toolbar button, or press `0` |
| **Zoom +/-** | Toolbar buttons, or `+` / `-` keys |

Paste any Mermaid code — with or without ```` ```mermaid ```` fences — and it renders instantly. Your code stays in `localStorage`, never leaves your machine.

---

## The Privacy Argument

If you work with sensitive system architectures, client names, internal project codenames, or staffing pipeline data, **you should not be pasting diagrams into hosted web applications**. 

A local single-file viewer eliminates the attack surface entirely:
- No network requests to third-party servers (mermaid.js is loaded from CDN once, can be saved locally for full offline use).
- No telemetry, no cookies, no analytics.
- Your diagram data never leaves your filesystem.

For teams handling government projects, financial systems, or classified infrastructure — this is not optional, it is a compliance requirement.

---

*The viewer source is available in the blog post's companion files. The LLM that built it still can't draw a diagram without human intervention — but at least now we have a place to fix them locally.*
