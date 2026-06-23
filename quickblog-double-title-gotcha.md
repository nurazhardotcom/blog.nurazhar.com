Title: Quickblog Gotcha: Why Your Post Titles Are Displaying Twice
Date: 2026-06-14
Tags: clojure, automation, web-development, simplicity
Description: A quick troubleshooting guide on how Quickblog handles title metadata rendering, and how to programmatically resolve double H1 headers.

---

When migrating articles from other static site generators (like Hugo, Jekyll, or standard GitHub-flavored Markdown) to **[Quickblog](https://github.com/borkdude/quickblog)**, you might run into a jarring visual bug: your post titles show up twice at the top of the page.

Here is why it happens, and how to programmatically fix it across your entire codebase.

---

## The Root Cause: Template vs. Body

In standard Markdown styling, it is natural to start your file with an H1 header matching your title:

```markdown
Title: My Great Post
Date: 2026-06-14
---
# My Great Post

This is the content of my post...
```

However, Quickblog uses a rendering template (typically found in `templates/post.html`) to dynamically construct your posts. If you inspect the default template, you will see it already wraps your metadata `{{title}}` in an H1 block:

```html
<h1>
  {% if post-link %}<a href="{{post-link}}">{% endif %}
  {{title}}
  {% if post-link %}</a>{% endif %}
</h1>
{{body | safe}}
```

Because Quickblog inserts the title dynamically, any `# Title` header you manually write inside the Markdown body will result in a double title on the compiled HTML page.

---

## The Fix: Programmatic Refactoring

Instead of manually editing dozens of markdown files to delete the redundant H1 header, you can clean them all up instantly using a quick Python regex script. 

Run this command in your repository root:

```bash
python3 -c '
import glob, re
for f in glob.glob("posts/*.md"):
    with open(f, "r") as file:
        content = file.read()
    # Remove the first H1 header line and any trailing empty lines immediately after it
    new_content = re.sub(r"(?m)^# .*\n\n?", "", content, count=1)
    with open(f, "w") as file:
        file.write(new_content)
'
```

This script:
1. Loops through every Markdown file in your `posts/` folder.
2. Uses a regex pattern (`(?m)^# .*\n\n?`) to find only the first occurrence of an H1 header (`# `) at the beginning of a line.
3. Removes it along with any trailing blank lines.
4. Rewrites the file, leaving your metadata header and standard body intact.

Once completed, run `bb quickblog render` to rebuild the pages, and the layout will render perfectly with a single title block.
