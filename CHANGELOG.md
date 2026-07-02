# Changelog

All notable changes to nurazhar.com will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project (a rolling-release personal site) uses ISO-date section
headers in lieu of SemVer tags.

## [Unreleased]

## [2026-07-01] — content sprint: Scrum series, mobile D2, validator rewrite, baked CI image

### Added

- **`9d0043e`** Scrum & Agile blog series — 11 new posts with hand-crafted D2 diagrams covering Agile from primer to comparison to ceremony deep-dives. ([9d0043e](https://gitlab.com/nurazhar/homepage/-/commit/9d0043e), [36aca68](https://gitlab.com/nurazhar/homepage/-/commit/36aca68), [f0a2337](https://gitlab.com/nurazhar/homepage/-/commit/f0a2337))
- **`5bd1571`**, **`bf72c7f`** Smoke test for the link validator — `bb test` runs `scripts/validate_links.clj` against a temp fixture in `tests/.test-workspace/` with two scenarios: `:sad` (1 known-broken ref → exit 1) and `:happy` (every internal ref resolves → exit 0). Future refactors of the validator are caught in both under-flagging and over-flagging directions.
- **`36aca68`** Blog conventions doc — `AGENTS.md` now captures the in-house blog format: D2/SVG diagrams (no PNG/Playwright), conversational H1, personal hook, short paragraphs, bold key terms, no TOC/footnotes/fluff, single-line takeaway.
- **`2f1ecc3`** Tracked build context — [`Dockerfile`](Dockerfile) (`babashka` + `d2` + `pandoc` on `eclipse-temurin:25-jre-alpine` base, ~280MB) and [`.dockerignore`](.dockerignore) (excludes `.git`, `public/`, `target/`) now live in the repo so any fork can build the same way.

### Changed

- **`2f1ecc3`** CI image is now baked once per push — replaces the per-pipeline `apk add + curl|sh` dance with a tracked `Dockerfile`. Published to the GitLab Container Registry as both `:latest` (cache-from for the next run) and `:$CI_COMMIT_SHORT_SHA` (atomic per pipeline). The Pages job consumes the SHA-pinned image — pipeline drops from ~90s cold to ~35s cached.
- **`146ee43`** Pre-commit hook now runs the full chain — `bb build` → `bb test` → `bb validate-links` (previously just `validate-links`). Validator-logic regressions are caught at commit time instead of waiting for CI.
- **`d727660`**, **`6e5aecb`** Link validator rewritten in pure Babashka Clojure — behavioural parity with the old Python script (counts Internal / MD / Broken, exits 1 on any broken ref, strips `?query#fragment`, skips external schemes including `mailto:` / `tel:` / `data:`). Uses `babashka.fs` only. `scripts/validate_links.py` deleted.

### Fixed

- **`5574a3c`**, **`1427a09`**, **`2a80b7e`**, **`596fd88`**, **`3dff5b0`**, **`b7a3c79`** Mobile D2 / SVG readability — pure-CSS tuning plus 2× font-size regeneration: standalone D2 SVGs scroll horizontally with `min-width`; figure height capped with internal scroll (`clamp(500px, 75vh, 80vh)` keeps small phones breathable and avoids generous white space on tablets); the cap is scoped to `:has(img[src$=".svg"])` so future photo figures are unaffected. Standalone SVG files now copied to `public/` so pandoc-rendered `<img>` references resolve.
- **`7e85e5c`** 6 pre-existing broken internal refs in `public/` — `LICENSE` is now shipped to `public/` (README badge + footer link resolve); `README.md`'s internal `./src`, `./bb.edn`, `./.gitlab-ci.yml` links re-pointed to absolute GitLab URLs (those are config files, not user-facing content); `agile-project-management-scrum-deep-dive.svg` (missing root hero) replaced with an inline D2 block, which `process-d2-blocks` in `src/site/fabricate/dev/dev.clj` renders deterministically on every build. `bb validate-links` now exits 0 across the whole generated site, removing the last `--no-verify` excuse.
- **`dba436e`**, **`bb26613`** Duplicate diagram references in the Scrum series — each post was embedding the hero SVG twice (once inline, once via `<img>`); de-duped across the 11 new Scrum posts plus the ceremonies deep-dive.

### Housekeeping

- **`c352a55`** `chore: add remaining untracked files` ([c352a55](https://gitlab.com/nurazhar/homepage/-/commit/c352a55))

[Unreleased]: https://gitlab.com/nurazhar/homepage/-/compare/main...HEAD
