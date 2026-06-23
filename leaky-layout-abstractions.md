Title: Leaky Layout Abstractions: Fixing Template Scope Leaks in Static Site Compilers
Date: 2026-06-15
Tags: architecture, babashka, clojure, web, automation, build
Description: A technical post-mortem on how custom standalone layouts leak unparsed compiler directives to the client, and how to resolve it at the pipeline layer without dependency bloat.

---

When building static sites, reusing a master template (like `base.html`) is standard practice. The layout defines the HTML head, script inclusions, navigation headers, and footer. 

However, when you step outside the boundaries of your static site generator's default compiler to build **custom standalone pages** (such as a public analytics dashboard or a Kanban board), you introduce a classic developer pitfall: **the template scope leak**.

Here is a look at how this bug manifests, why it happens, and how to resolve it at the build pipeline layer in Clojure/Babashka without pulling in bloated template engine libraries.

---

## 1. The Anatomy of the Leak

In our Babashka-native blog compiler (`quickblog`), standard markdown posts are parsed, and unused conditional tags (like a GitHub link or a Twitter handle) are cleanly stripped out.

But when rendering our custom `/stats.html` and `/kanban.html` dashboards, we bypassed the markdown parser and did a raw find-and-replace on the base template:

```clojure
;; Bypassing the compiler layout context:
(clojure.string/replace base-html "{{body | safe }}" stats-body)
```

Because this operation was a simple string substitution, **none of the template compiler's conditional logic was evaluated.** The browser received raw, unparsed control directives directly in the markup:

```html
{% if discuss-link %}<a href="{{discuss-link}}">Discuss</a>{% endif %}
{% if twitter-handle %}<a href="https://twitter.com/{{twitter-handle}}">Twitter</a>{% endif %}
```

The browser ignored the unknown template syntax wrapper and rendered the text inside. Suddenly, disabled links ("Discuss", "Twitter", "About") leaked onto the live layout as raw, broken navigation menu items.

---

## 2. The Heavy Fix vs. The Pragmatic Fix

To resolve this, you have two architectural choices:

### Approach A: Pull in a Templating Library (Bloat)
You could add a full template compilation dependency (like Selmer or stencil) to your build runner. While robust, this adds compile-time dependency overhead, slows down local file-watchers, and introduces library complexity just to render two pages.

### Approach B: Clean the Output at the Pipeline Layer (Zero-Dependency)
Since the master compiler already strips these values for standard pages, we can write a simple, stateless regex post-processing filter directly inside our Babashka task script (`bb.edn`):

```clojure
(let [clean-template (fn [s]
                       (-> s
                           ;; 1. Strip out unparsed JTE control blocks
                           (clojure.string/replace (re-pattern "\\{%.*?%\\}") "")
                           ;; 2. Strip out remaining unused placeholder vars
                           (clojure.string/replace (re-pattern "\\{\\{favicon-tags \\| safe\\}\\}") "")
                           (clojure.string/replace (re-pattern "\\{\\{sharing\\..*?\\}\\}") "")
                           ;; 3. Remove conditional links that leaked
                           (clojure.string/replace (re-pattern "<a class=\"page-link\" href=\"\\{\\{discuss-link\\}\\}\">Discuss</a>") "")
                           (clojure.string/replace (re-pattern "<a class=\"page-link\" href=\"https://twitter.com/\\{\\{twitter-handle\\}\\}\">\\s*Twitter\\s*</a>") "")
                           (clojure.string/replace (re-pattern "<a class=\"page-link\" href=\"\\{\\{about-link\\}\\}\">About</a>") "")
                           ;; 4. Normalize page suffix
                           (clojure.string/replace (re-pattern "\\{\\{page-suffix\\}\\}") ".html")))]
  ;; Apply compiler filter to the rendered output:
  (spit "public/kanban.html" (clean-template raw-rendered)))
```

---

## 3. Resume Perspective: Why It Matters

This debugging exercise demonstrates three key engineering principles:

1. **Understanding Compiler Stages**: Recognizing that string-level replacement does not inherit template-level parsing rules.
2. **Avoiding Dependency Inflation**: Resisting the urge to add packages to solve a basic text-parsing problem. A few lines of targeted Clojure string replacements maintain a lightweight, fast, and zero-maintenance build pipeline.
3. **Defense-in-Depth UI**: Ensuring client-facing pages never render unparsed server-side variables or control directives.
