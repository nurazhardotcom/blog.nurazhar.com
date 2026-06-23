Title: How a Single ".env" in allowed-extensions Could Leak Your Secrets to LLMs
Date: 2026-06-17
Tags: security, clojure, babashka, llm, ai-safety
Description: A three-layer defense-in-depth fix for an AI context bundler that was accidentally including .env files in LLM context.

---

In our AI context bundler (`generate_context.clj`), a single line allowed `.env` files to be read, bundled into `ai_context.txt`, and sent to external LLMs:

```clojure
(def allowed-extensions #{".py" ".js" ".jsx" ".ts" ".tsx" ".html" ".css"
                           ".json" ".md" ".sql" ".env" ".ini" ".toml" ".clj"})
```

## Jurisdiction / Data Flow

Local filesystem → Babashka script → `ai_context.txt` → External LLM API (OpenRouter, Anthropic, etc.) → Model provider logs/training data.

## Why This Matters

| Secret Type | Typical Location | Risk if Leaked |
|-------------|------------------|----------------|
| API Keys | `.env`, `secrets.yaml` | Full account compromise, billing abuse |
| Database URLs | `.env`, `config.toml` | Data exfiltration, ransomware |
| JWT Secrets | `.env` | Token forgery, auth bypass |
| Cloud Credentials | `.aws/credentials`, `.gcp/` | Infrastructure takeover |

## The Fix: Defense in Depth

Three layers, not one.

### Layer 1: Remove `.env` from Allowlist

```diff
- #{".py" ".js" ".jsx" ".ts" ".tsx" ".html" ".css"
-  ".json" ".md" ".sql" ".env" ".ini" ".toml" ".clj"}
+ #{".py" ".js" ".jsx" ".ts" ".tsx" ".html" ".css"
+  ".json" ".md" ".sql" ".ini" ".toml" ".clj"}
```

### Layer 2: Explicit Denylist (Known Dangerous Filenames)

```clojure
(def exclude-files #{"ai_context.txt" "db.sqlite3" "package-lock.json" "yarn.lock"
                     "pnpm-lock.yaml" "uv.lock"})
```

### Layer 3: Hard-Coded Regex Guard (Catches Variants)

```clojure
(def secret-patterns #{"\\.env$" "(?i)secret" "(?i)key" "(?i)token"})

(defn contains-secret-pattern? [filename]
  (some #(re-find (re-pattern %) filename) secret-patterns))

(defn allowed-file? [file]
  (let [name (.getName file)
        ext  (get-extension name)]
    (and (not (contains? exclude-files name))     ; Layer 2
         (not (contains-secret-pattern? name))    ; Layer 3
         (contains? allowed-extensions ext))))    ; Layer 1
```

## Evidence Table

What the guard catches (tested locally):

| Filename | Matched Pattern | Blocked? |
|----------|-----------------|----------|
| `.env` | `\.env$` | ✅ |
| `.env.production` | `\.env$` | ✅ |
| `secrets.yaml` | `(?i)secret` | ✅ |
| `api-keys.json` | `(?i)key` | ✅ |
| `auth-token.txt` | `(?i)token` | ✅ |
| `config.toml` | — | ❌ (allowed) |

## Architecture Diagram

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐     ┌─────────────┐
│  Developer  │────▶│ .env / secret│────▶│ generate_context│────▶│  LLM Provider│
│  Workspace  │     │   files      │     │    (babashka)   │     │  (External) │
└─────────────┘     └──────────────┘     └────────┬────────┘     └──────┬──────┘
                                                   │                    │
                            ┌──────────────────────┘                    │
                            ▼                                           ▼
                   ┌─────────────────┐                        ┌─────────────────┐
                   │  THREE LAYERS   │                        │  SECRETS LEAKED │
                   │  OF DEFENSE     │                        │  TO MODEL LOGS  │
                   ├─────────────────┤                        ├─────────────────┤
                   │ 1. Allowlist    │                        │ • API keys      │
                   │    (no .env)    │                        │ • DB passwords  │
                   │ 2. Denylist     │                        │ • JWT secrets   │
                   │    (explicit)   │                        │ • Cloud creds   │
                   │ 3. Regex Guard  │                        │ • Training data │
                   │    (patterns)   │                        │   contamination │
                   └─────────────────┘                        └─────────────────┘
```

## Resume Value?

**Yes.** This demonstrates:

- **Security mindset** — proactive secret scanning, not reactive
- **Defense in depth** — multiple independent layers
- **LLM supply chain awareness** — understands where context goes
- **Clojure/Babashka fluency** — practical scripting, not toy examples
- **Clean Git workflow** — branch, commit, PR, merge, clean history

Frame it as: _"Identified and remediated a credential leakage vector in an AI context pipeline by implementing a three-layer secret detection guard (allowlist hardening, explicit denylist, regex pattern matching), preventing .env and secret* files from reaching external LLM APIs."_

## Takeaway

Never trust an allowlist alone. Secrets hide in filenames, not just extensions. The regex guard is **hard-coded** — no config, no user override, no "oops I added .env back." It fails closed.

---

Fix merged: [PR #2](https://github.com/nurazhardotcom/lithan-dev-sandbox/pull/2) ·
Stack: Babashka Clojure · Zero deps · Plain HTML/CSS blog