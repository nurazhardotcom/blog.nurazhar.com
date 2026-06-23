Title: SEO, AEO, GEO: Making a Static Blog Machine-Readable
Date: 2026-06-21
Tags: seo, aeo, geo, json-ld, structured-data, sitemap, meta-tags, optimization
Description: A technical walkthrough of adding JSON-LD structured data, sitemap generation, per-post OG tags, and semantic metadata to a quickblog static site. Moving beyond "SEO for search engines" toward Answer Engine and Generative Engine Optimization.

---

SEO is dead. Long live SEO.

The old model — keyword density, backlinks, meta keyword tags — has been replaced by three overlapping optimization targets:

1. **SEO** — Search Engine Optimization (Google, Bing, DuckDuckGo). Traditional ranking signals, still relevant.
2. **AEO** — Answer Engine Optimization (Google AI Overviews, Perplexity, Bing Copilot). Structured data that machines extract to answer questions directly.
3. **GEO** — Generative Engine Optimization (ChatGPT, Claude, Gemini). Content that LLMs cite in generated answers.

All three share one requirement: **machine-readable semantic markup**. A beautiful page with perfect prose but no structured data is invisible to all three.

This post documents how I turned a minimal `quickblog` static site into a machine-readable knowledge graph with about 50 lines of template changes and a sitemap generator.

---

## 1. The Starting Point

The blog runs on [quickblog](https://github.com/borkdude/quickblog) — a Clojure/Babashka static site generator. The template system uses Selmer (Django/Jinja2-like syntax). Before optimization, the `<head>` looked like this:

```html
<title>Nur Azhar&#39;s</title>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="title" content="Nur Azhar&#39;s">
<meta property="og:title" content="Nur Azhar&#39;s">
<meta property="og:type" content="website">
<meta name="description" content="Blog in an era of artificial intelligence">
<meta property="og:description" content="Blog in an era of artificial intelligence">
<meta name="twitter:card" content="summary">
```

Problems:
- Title was just "Nur Azhar's" — no keywords, no context
- Description was vague — "Blog in an era of artificial intelligence"
- No JSON-LD structured data at all
- No sitemap.xml
- No Open Graph image
- No author meta tag
- OG type was always "website" even on post pages
- No locale or site name declared

---

## 2. JSON-LD Structured Data (AEO/GEO)

This is the most important change. JSON-LD (JavaScript Object Notation for Linked Data) tells machines what your page *is*, not just what it *says*.

### WebSite Schema (every page)

Added to `templates/base.html` in the `<head>`:

```html
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "WebSite",
  "name": "Nur Azhar's Blog",
  "url": "https://blog.nurazhar.com",
  "description": "{{blog-description}}",
  "author": {
    "@type": "Person",
    "name": "Nur Azhar"
  }
}
</script>
```

This tells search engines and LLMs: "This is a blog. It's about [description]. Written by Nur Azhar." Every page gets this — it costs nothing and establishes authorship and topic.

### BlogPosting Schema (post pages only)

```html
{% if date %}
<script type="application/ld+json">
{
  "@context": "https://schema.org",
  "@type": "BlogPosting",
  "headline": "{{title}}",
  "description": "{{sharing.description}}",
  "datePublished": "{{date}}",
  "author": {
    "@type": "Person",
    "name": "Nur Azhar"
  },
  "url": "{{sharing.url}}",
  "mainEntityOfPage": {
    "@type": "WebPage",
    "@id": "{{sharing.url}}"
  }
}
</script>
{% endif %}
```

The `{% if date %}` conditional ensures this only renders on post pages (the `date` variable is only bound in post context). This tells machines: "This specific page is a blog post. It was published on this date. It has this headline. It covers this topic."

**Why this matters for AEO/GEO:**
- Google AI Overviews explicitly uses schema.org markup to surface content in answers
- Perplexity's `pro` search mode prioritizes pages with structured data
- ChatGPT's web browsing mode extracts JSON-LD for citation
- Claude's webpage reading parses schema markup for context

---

## 3. Title Tags with Context (SEO)

The original title tag was just "Nur Azhar's" — useless for search. I changed it to include a descriptive tagline suffix:

```html
<title>{{title}} — Bitcoin, AI Agents, Protocol Engineering</title>
```

On the index page this renders as:
```
Nur Azhar's — Bitcoin, AI Agents, Protocol Engineering
```

On a post page:
```
BSV is Bitcoin: The 16-Year Restoration — Bitcoin, AI Agents, Protocol Engineering
```

The pattern: **specific content first, site context second**. This is the recommendation from Google's SEO documentation and it helps both search engines and social previews.

---

## 4. Per-Page OG Type (SEO/AEO)

The `og:type` was hardcoded to `website`. Changed to be context-aware:

```html
<meta property="og:type" content="{% if date %}article{% else %}website{% endif %}">
```

Post pages now correctly declare themselves as `article` type, which affects how social platforms and AI crawlers interpret the content.

---

## 5. OG Locale and Site Name

```html
<meta property="og:locale" content="en_SG">
<meta property="og:site_name" content="Nur Azhar's Blog">
```

`og:locale` tells platforms the language and regional variant. `og:site_name` establishes brand context when individual pages are shared in isolation.

---

## 6. Author Meta Tag

```html
{% if sharing.author %}
    <meta name="author" content="{{sharing.author}}">
{% else %}
    <meta name="author" content="Nur Azhar">
{% endif %}
```

Quickblog supports per-post `Twitter-Handle` frontmatter for guest authors. The fallback is the blog owner's name. Author attribution improves expertise signals (E-E-A-T in Google's rating guidelines).

---

## 7. Sitemap.xml Generation

Quickblog doesn't generate a sitemap natively. I added one in the post-render hook in `bb.edn`:

```clojure
(let [collect-html (fn [dir prefix]
                     (let [f (java.io.File. dir)]
                       (when (.isDirectory f)
                         (->> (.listFiles f)
                              (filter (fn [x] (.isFile x)))
                              (map (fn [x] (.getName x)))
                              (filter (fn [x] (.endsWith x ".html")))
                              (map (fn [x] (str "<url><loc>" blog-root prefix x "</loc></url>")))))))
      html-files (concat
                   (collect-html "public" "/")
                   (collect-html "public/tags" "/tags/"))
      sitemap (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                   "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
                   "<url><loc>" blog-root "/</loc></url>\n"
                   "<url><loc>" blog-root "/archive.html</loc></url>\n"
                   (->> html-files
                        (remove (fn [s] (or (.contains s "/index.html")
                                             (.contains s "/stats.html")
                                             (.contains s "/kanban.html"))))
                        (sort)
                        (clojure.string/join "\n"))
                   "\n</urlset>")]
  (spit "public/sitemap.xml" sitemap))
```

This scans the rendered `public/` directory for all HTML files (including tag pages), excludes system pages (stats, kanban, duplicate index), and generates a valid XML sitemap. Google Search Console uses this to discover all pages.

The `collect-html` function is generic — it takes a directory and a URL prefix, making it easy to add more subdirectories in the future.

---

## 8. Sitemap Link in HTML

Added to the `<head>` so crawlers can find it:

```html
<link rel="sitemap" type="application/xml" href="{{relative-path | safe}}sitemap.xml">
```

This is a hint for web crawlers and is referenced in the sitemaps.org protocol.

---

## 9. Blog Description Rewrite

From the vague "Blog in an era of artificial intelligence" to something searchable and specific:

```clojure
:blog-description "Bitcoin protocol engineering, AI agents, decentralized infrastructure, overlay networks, and the Internet of Agents. Engineering on BSV."
```

This description appears in:
- The `<meta name="description">` tag on the index page
- The WebSite JSON-LD schema
- The site header subtitle
- Social sharing previews

Keywords should describe what the blog *actually covers*, not make aspirational claims.

---

## 10. Handling Custom Pages (Stats, Kanban)

The blog has two standalone pages (`stats.html` and `kanban.html`) that are generated through a custom rendering path rather than quickblog's template engine. These pages use `clean-template` — a function that strips Selmer template tags and replaces leftover variables.

The BlogPosting JSON-LD would leak into these pages because the `{% if date %}` conditional is stripped by the clean-template regex. Solution: explicitly strip the entire BlogPosting block:

```clojure
(clojure.string/replace (re-pattern "(?s)<!-- JSON-LD Structured Data: BlogPosting -->.*?</script>") "")
```

The `(?s)` flag enables dotall mode so `.` matches newlines, and `.*?` is non-greedy to stop at the first `</script>`. The clean-template function runs on both stats and kanban pages, so this block is stripped from both.

---

## 11. The Full Checklist

Here is every change made, in priority order for maximum impact:

| Change | Impact | Effort |
|--------|--------|--------|
| JSON-LD WebSite schema (all pages) | AEO/GEO — establishes site identity | 10 lines |
| JSON-LD BlogPosting schema (posts) | AEO/GEO — per-post machine-readable metadata | 15 lines |
| Sitemap.xml generation | SEO — crawler discovers all pages | 25 lines |
| Improved title tag format | SEO — keyword context in title | 1 line |
| Per-page OG type (article/website) | Social sharing + AI crawlers | 1 line |
| OG locale + site name | Social sharing context | 2 lines |
| Author meta tag | E-E-A-T signal | 4 lines |
| Sitemap link in HTML | Crawler discovery | 1 line |
| Better blog description | SEO + semantic context | 1 line |
| OG image per post | Social sharing + AEO (TODO) | Frontmatter only |

---

## 12. What's Next: OG Image

The one gap: no Open Graph image yet. Social platforms and AI crawlers prefer cards with images. The plan is to create a 1200x630px template image and add per-post images via the `Image:` frontmatter field (which quickblog supports natively).

The `:blog-image` option in quickblog handles the default for index/archive/tags pages, while per-post `Image:` frontmatter overrides it for individual posts.

---

## 13. Measuring Results

These changes went live today, so no long-term data yet. But the immediate effects are measurable:

- **Google Search Console**: submit sitemap for indexing, check for JSON-LD errors in the Rich Results report
- **Schema.org Validator**: https://validator.schema.org/ — paste a post URL to verify BlogPosting markup
- **Facebook Sharing Debugger**: https://developers.facebook.com/tools/debug/ — check OG tags render correctly
- **Perplexity**: search for a post title — does it cite the blog?
- **ChatGPT browsing**: ask it about a topic covered in the blog — does it reference the content?

The structured data doesn't guarantee ranking or citation. But without it, the machine has to guess what your page is. With it, you've stated your identity, your content type, and your topics in a format that every major AI system reads natively.

---

*This blog runs on [quickblog](https://github.com/borkdude/quickblog) + [Babashka](https://babashka.org/), deployed to GitHub Pages. Source code at [github.com/nurazhardotcom/blog.nurazhar.com](https://github.com/nurazhardotcom/blog.nurazhar.com).*
