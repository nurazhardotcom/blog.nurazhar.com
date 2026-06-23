Title: Static Site Resume and Kitchen Sink
Date: 2026-06-21
Tags: meta, static-site, portfolio, engineering
Description: A static blog with live metrics serves as both portfolio and credibility signal — kanban board and stats page walkthrough.

---

As a software engineer, your resume is only as strong as your ability to demonstrate real work. A static blog with live metrics serves as both portfolio and credibility signal.

I recently cleaned up two pages on my static blog that serve this purpose — the Kanban board and the stats page.

## The Kanban Board

A static kanban board from a JSON file. No database, no server runtime. The tasks are compiled directly into the HTML during the build step.

```clojure
;; From bb.edn — compiled at render time
(let [kanban-body (slurp "templates/kanban.html")
      kanban-state (slurp "kanban_state.json")
      kanban-rendered (clean-template
        (-> base
            (clojure.string/replace "{{body | safe }}" kanban-body)
            ...))]
  (spit "public/kanban.html" kanban-rendered))
```

The state is injected as a JSON blob in the HTML, and JavaScript renders the columns client-side. No API calls, no loading states — just a snapshot of current tasks, version-controlled alongside the code.

This is useful for a resume because it shows:

- You track work systematically
- You understand the difference between build-time and runtime
- Your project management is transparent and visible

## The Stats Page

The original stats page had three summary boxes (Total Views, Tracked Pages, Primary Country), a top-pages table, a referrer table, and a country distribution chart. Three data sources, five visual elements — too much noise.

I stripped it to just the country distribution:

```html
<div id="analytics-app">
  <h2>Visitor Geography</h2>
  <div id="countries-list"></div>
</div>
```

Why just countries? Because for a personal resume blog, the single most informative metric is geographic diversity of readership. Page view counts and referrer sources are vanity metrics — they don't tell an employer anything useful. But seeing readers from 15+ countries signals reach that a static personal blog doesn't naturally have.

The API call is unchanged (a Cloud Function behind the scenes), but the rendering is minimal:

```javascript
renderCountries(data) {
  const maxViews = Math.max(...data.countries.map(c => parseInt(c.views, 10)));
  data.countries.forEach(c => {
    const pct = (views / maxViews) * 100;
    // Render country name, view count, horizontal bar
  });
}
```

No charts library. No dependencies. Just CSS bars proportional to the max.

## The Resume Signal

Both of these pages sit at `blog.nurazhar.com` — my static blog. They demonstrate:

| Signal | What it proves |
|---|---|
| Static site generation | You understand build pipelines |
| Cloud Function integration | You can wire serverless backends to static frontends |
| API design | You choose what data to expose and how |
| Constraints-driven UI | You strip features, not add them |

The kanban page is at `/kanban.html`. The stats page is at `/stats.html`. Both are generated from the same Clojure Babashka build script that renders the rest of the blog.

Most engineers build up. Building down — removing features, simplifying data, reducing dependencies — is harder and more valuable.
