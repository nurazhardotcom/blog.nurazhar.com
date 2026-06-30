# nurazhar.com

> Source for [**nurazhar.com**](https://nurazhar.com) — my personal blog & portfolio.
> Essays on AI agents, Bitcoin SV, local-first software, security, and engineering craft.

[![status: active](https://img.shields.io/badge/status-active-success)](https://nurazhar.com)
[![built with babashka](https://img.shields.io/badge/built%20with-babashka-orange)](https://github.com/babashka/babashka)
[![license: MIT](https://img.shields.io/badge/license-MIT-blue)](./LICENSE)

## About

This isn't a off-the-shelf static-site generator. The build pipeline is a small
**custom generator written in [Babashka](https://github.com/babashka/babashka)
(Clojure)** — see [`src/`](./src) and [`bb.edn`](./bb.edn). Posts are plain
Markdown (`*.md`) at the repo root, rendered with `pandoc` and diagrams with
[`d2`](https://d2lang.com), then published via GitLab Pages.

## Build

```bash
bb build      # render markdown -> public/
```

Deploy is automatic on push to `main` via [`.gitlab-ci.yml`](./.gitlab-ci.yml)
(GitLab Pages, artifact: `public/`).

## Featured writing

A few posts that best represent what I write about:

- [`15-minute-serverless-vapt.md`](./15-minute-serverless-vapt.md) — a fast vulnerability assessment walkthrough
- [`5mb-to-169kb-blog-performance-audit.md`](./5mb-to-169kb-blog-performance-audit.md) — cutting page weight 30×
- [`ai-audit-github-disaster.md`](./ai-audit-github-disaster.md) — auditing an AI-driven GitHub disaster
- [`aur-audit-pgp-keys-wkhtmltopdf.md`](./aur-audit-pgp-keys-wkhtmltopdf.md) — PGP keys & the AUR (companion to [`aur-audit`](https://github.com/nurazhardotcom/aur-audit))
- [`applying-agile-scrum-to-headhunter-agent.md`](./applying-agile-scrum-to-headhunter-agent.md) — process applied to a real project
- [`bitcoin-ipv6-original-design-mandala-he.md`](./bitcoin-ipv6-original-design-mandala-he.md) — Bitcoin's original IPv6 design

Full archive of **152 posts** at [nurazhar.com](https://nurazhar.com).

## License

[MIT](./LICENSE) © Nur Azhar
