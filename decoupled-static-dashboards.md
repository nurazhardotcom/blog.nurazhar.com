Title: Decoupled Static Dashboards: Building Transparent roadmaps in Public
Date: 2026-06-15
Tags: architecture, babashka, clojure, web, automation, minimalism
Description: How to build and serve interactive, public project roadmaps natively to readers at zero compute cost using static compiler pipelines.

---

When you decide to **build in public**, transparency is the primary asset. You want your readers, users, and future contributors to see exactly what features are completed, what is in active development, and what is coming next.

But building this dashboard traditionally introduces unnecessary architectural choices:
- Spin up a database to host card states? (Adds connection limits and DB maintenance).
- Deploy a dynamic API server? (Breaks the $0 idle server cost baseline).
- Embed a third-party corporate widget like GitHub Projects? (Leaks reader telemetry and looks generic).

To keep our architecture clean and completely free of operational overhead, we decided to host our roadmap board natively on the blog under `/kanban.html`.

Here is the exact decoupled design pattern we used to build it.

---

## 1. The Zero-Entropy Pipeline

Instead of running an active backend database, we treat our repository as the **Single Source of Truth**. The tasks are stored inside a plain JSON manifest file (`kanban_state.json`) directly in the blog's code structure:

```json
{
  "tasks": [
    {
      "id": "task-1",
      "title": "Refactor Payout splits to Decoupled Settlement",
      "desc": "Splitting 85/15 database writes immediately instead of pooling batch structures.",
      "status": "done"
    }
  ]
}
```

Every time we write code or complete a task locally, we simply edit this JSON file. 

---

## 2. Compile-Time HTML Injection

To render this board, we don't fetch the JSON dynamically over the network when a user opens the page (which would require API calls and trigger cold starts). Instead, the tasks are injected **directly into the HTML code at compile-time** by our Babashka static site builder.

In `bb.edn`, we read both the HTML template and the JSON file:

```clojure
(let [base (slurp "templates/base.html")
      kanban-body (slurp "templates/kanban.html")
      kanban-state (slurp "kanban_state.json")
      rendered (-> base
                   (clojure.string/replace "{{title}}" "lagu-lagu")
                   (clojure.string/replace "{{body | safe }}" kanban-body)
                   (clojure.string/replace "{{kanban-state | safe}}" kanban-state))]
  (spit "public/kanban.html" rendered))
```

Inside [templates/kanban.html](https://github.com/nurazhardotcom/blog.nurazhar.com/blob/main/templates/kanban.html), a simple script parses the pre-injected JSON variable and maps the elements to their CSS columns on the fly:

```javascript
(function() {
  // Directly compiled from local JSON during rendering
  const state = {{kanban-state | safe}};
  
  function renderBoard() {
    state.tasks.forEach(t => {
      const column = document.getElementById('tasks-' + t.status);
      // Create and append task card element...
    });
  }
  renderBoard();
})();
```

---

## 3. The Execution Flow

<div style="display: flex; flex-direction: column; gap: 8px; max-width: 420px; margin: 24px auto; font-size: 14px; line-height: 1.5;">

<div style="border: 2px solid #6366f1; background: #eef2ff; border-radius: 12px; padding: 14px; text-align: center;">
  <strong style="font-size: 15px;">💻 Developer (Local laptop)</strong><br>
  <span style="font-size: 12px; color: #64748b;">1. Edit kanban_state.json → Git commit & push</span>
</div>

<div style="text-align: center; font-size: 22px; color: #64748b;">↓</div>

<div style="border: 2px solid #0891b2; background: #ecfeff; border-radius: 12px; padding: 14px; text-align: center;">
  <strong style="font-size: 15px;">🐙 Git Commit & Push</strong><br>
  <span style="font-size: 12px; color: #64748b;">2. Trigger Workflow</span>
</div>

<div style="text-align: center; font-size: 22px; color: #64748b;">↓</div>

<div style="border: 2px solid #d97706; background: #fef3c7; border-radius: 12px; padding: 14px; text-align: center;">
  <strong style="font-size: 15px;">☁️ GitHub Actions</strong><br>
  <span style="font-size: 12px; color: #64748b;">bb quickblog render</span><br>
  <span style="font-size: 12px; color: #64748b;">3. Compile JSON into HTML</span>
</div>

<div style="text-align: center; font-size: 22px; color: #64748b;">↓</div>

<div style="border: 2px solid #059669; background: #d1fae5; border-radius: 12px; padding: 14px; text-align: center;">
  <strong style="font-size: 15px;">🌐 GitHub Pages CDN</strong><br>
  <span style="font-size: 12px; color: #64748b;">4. Serve 100% static file</span>
</div>

<div style="text-align: center; font-size: 22px; color: #64748b;">↓</div>

<div style="border: 2px solid #7c3aed; background: #f5f3ff; border-radius: 12px; padding: 14px; text-align: center;">
  <strong style="font-size: 15px;">👥 Blog Reader</strong>
</div>

</div>

1. **Commit**: We modify our local `kanban_state.json` file as we code.
2. **Compile**: Pushing to GitHub triggers our Actions runner (`bb quickblog render`) which compiles the JSON data straight into the static HTML layout.
3. **Serve**: The completed static page is pushed to GitHub Pages. The reader loads a 100% pre-compiled HTML page from a globally cached CDN.

---

## The Takeaway

By choosing a **decoupled static design**, we get:
- **Zero Server Overhead**: Idle costs remain $0. No databases to manage.
- **Fast Load Times**: The browser doesn't wait for API roundtrips.
- **Developer Ownership**: You write your documentation, posts, and roadmap in plain text files using your favorite local editor. The compiler handles the presentation.

This is the exact setup now running live for **[lagu-lagu](https://blog.nurazhar.com/kanban.html)**.
