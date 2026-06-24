Title: Headhunter-Agent: Full Tech Stack of a Local-First Multi-Agent System
Date: 2026-06-24
Tags: clojure, mas, ai-agents, architecture, m2m, babashka, gemini, desktop, privacy, ed25519
Description: A deep dive into the full tech stack of headhunter-agent — a local-first, privacy-preserving multi-agent system for job hunting built in Clojure.

---

Headhunter-Agent is a **Local-First, Privacy-Preserving Multi-Agent System (MAS)** — a native Clojure desktop console that orchestrates a pipeline of AI agents to automate the entire job-hunting workflow: profiling, evaluation, interview prep, resume tailoring, and decentralized submission.

This post walks through the full tech stack end-to-end, from the JavaFX desktop shell down to the Ed25519 signing keys.

## Why Build This?

The job market runs on manual, browser-dependent workflows: paste your resume into portals, solve CAPTCHAs, re-type the same information across 50 sites, track applications in a spreadsheet. Headhunter-Agent inverts this:

- **Your data stays local.** No cloud, no analytics, no third-party storage.
- **Agents do the work.** A pipeline of Gemini-powered agents evaluates fit, generates strategy, and tailors outputs.
- **Machine-to-machine submission.** The M2M protocol lets agents apply directly — no browser, no portal.

It is designed to run entirely locally as a native Clojure Desktop GUI using **cljfx (JavaFX)**. No web browsers, no local web servers, and zero JavaScript.

## System Architecture

The system is organized into three interface layers (CLI, GUI, M2M) feeding into a shared pipeline of five modules, all backed by local data files:

```d2
direction: up

CLI: "CLI Layer\nbb.edn / core.clj" {
  C1: "bb profile"
  C2: "bb evaluate"
  C3: "bb interview"
  C4: "bb pdf"
  C5: "bb tracker"
}

GUI: "Desktop GUI\ncljfx / gui.clj" {
  G1: "Data Vault tab"
  G2: "Evaluator tab"
  G3: "Interview tab"
  G4: "Tracker tab"
}

M2M: "M2M Protocol\nm2m/*.clj" {
  M1: "Discovery\nDNS TXT lookup"
  M2: "Fetch\nJSON-LD posting"
  M3: "Verify\nEd25519 handshake"
  M4: "Submit\nsigned package"
}

Gateway: "Gemini API Gateway\nevaluator.clj"

Modules: "Pipeline Modules" {
  P1: "profiler.clj\nData Vault extraction"
  P2: "evaluator.clj\n3-Stage MAS"
  P3: "interview.clj\nSTAR story mapping"
  P4: "pdf.clj\nATS PDF compilation"
  P5: "tracker.clj\nApplication pipeline"
}

Data: "Local Data (User Layer)" {
  D1: "master-profile.edn"
  D2: "star-stories.edn"
  D3: "applications.md"
  D4: "cv.md"
  D5: "profile.yml"
}

Daemon: "Daemon MCP Server\ndaemon/core.clj" {
  DA1: "tools/list"
  DA2: "tools/call"
}

CLI -> Gateway
GUI -> Gateway
M2M -> Gateway
Gateway -> Modules
Modules -> Data
Daemon -> Modules
Daemon --> CLI: "bb daemon"
```

## Component Breakdown

### 1. Interface Layer — Desktop GUI (cljfx)

The primary interface is a native JavaFX window built with **cljfx** (878 lines). It provides four tabs in a single-window layout with a fixed sidebar:

- **Data Vault** — Paste raw LinkedIn/CV text, extract structured master profile and 8-12 STAR stories
- **JD Evaluator** — 3-stage MAS pipeline with color-coded score visualization (green >= 4.0, yellow >= 3.0, red < 3.0)
- **Interview Prep** — Generate STAR-mapped prep sheets
- **Pipeline Tracker** — Visual cards for each application with status badges

The GUI uses a **Nord Dark theme** (294 lines CSS) with slate-900 backgrounds, emerald/orange/red badge systems, and custom scrollbars. All heavy operations run in `future` threads with `Platform/runLater` for JavaFX thread safety.

### 2. Interface Layer — CLI (Babashka)

The CLI is defined in `bb.edn` and routes through `core.clj`:

```bash
bb profile   --extract /path/to/linkedin-dump.txt
bb evaluate  --file ./jds/defence-collective.txt
bb interview --file ./jds/defence-collective.txt
bb pdf       --file ./jds/defence-collective.txt
bb tracker   list
bb daemon    serve --port 8081
```

Same source code runs in both Babashka (native binary, millisecond startup) and JVM Clojure.

### 3. Multi-Agent Evaluator Pipeline

The core intelligence is a **3-stage sequential MAS pipeline** in `evaluator.clj`:

```d2
direction: right

JD: "Job Description"

A1: "Agent 1\nLegitimacy" {
  A1a: "Parse JD"
  A1b: "FCF compliance check"
  A1c: "Red flag detection"
}

A2: "Agent 2\nFit Analysis" {
  A2a: "Master Profile vs JD"
  A2b: "Gap analysis"
  A2c: "GO / NO-GO"
}

A3: "Agent 3\nCheat Sheet" {
  A3a: "Business model"
  A3b: "Tech stack"
  A3c: "Outreach strategy"
}

Report: "Markdown Report\nreports/*.md"

JD -> A1: "Stage 1"
A1 -> A2: "Stage 2 (with profile)"
A2 -> A3: "Stage 3"
A3 -> Report: "compiled output"
```

Each agent calls **Google Gemini** via HTTP POST with a carefully crafted system prompt:
- **Agent 1** — Parses JD, checks Fair Consideration Framework (FCF) legitimacy, identifies red flags
- **Agent 2** — Brutal comparison of Master Profile vs JD, exact gaps/strengths, GO/NO-GO
- **Agent 3** — Pre-interview cheat sheet: business model, tech stack, cold outreach strategy

Temperature varies by stage: 0.5 for evaluation, 0.2 for extraction and PDF generation.

```d2
direction: right

Gemini: "Gemini API"

P1: "profiler.clj\ntemp: 0.2" {
  P1a: "Extract master_profile"
  P1b: "Extract star_stories"
}

P2: "evaluator.clj\ntemp: 0.5" {
  P2a: "3 sequential agents"
  P2b: "Stage output feeds next"
}

P3: "pdf.clj\ntemp: 0.2" {
  P3a: "ATS-tailored JSON"
  P3b: "responseMimeType: json"
}

P4: "interview.clj" {
  P4a: "Map STAR stories"
  P4b: "5 most likely questions"
}

Gemini -> P1: "structured JSON"
Gemini -> P2: "evaluation 3-stage"
Gemini -> P3: "tailored resume"
Gemini -> P4: "interview prep"
```

### 4. M2M Protocol — Decentralized Job Applications

The M2M protocol is a **machine-to-machine job application pipeline** that eliminates browsers, CAPTCHAs, and manual portals. It defines four sub-protocols:

```d2
direction: right

Candidate: "Candidate Agent\nbb bb-m2m apply" {
  C1: "Ed25519 keypair"
  C2: "Master profile"
  C3: "STAR library"
}

Employer: "Employer Server\nm2m-enabled" {
  E1: "DNS TXT record"
  E2: "JSON-LD postings"
  E3: "Verify + respond"
}

A: "1. Discover\n_m2m-apply.employer\nTXT lookup" {
  A1: "endpoint URL"
  A2: "public key"
}

B: "2. Fetch\nGET /jobs.jsonld" {
  B1: "JobPosting (JSON-LD)"
  B2: "m2m:applyEndpoint"
  B3: "m2m:publicKey"
}

C: "3. MAS Evaluate\n(existing pipeline)" {
  C1: "Score + Report"
  C2: "GO or NO-GO"
}

D: "4. Submit\nPOST multipart" {
  D1: "Signed ApplicationPackage"
  D2: "Ed25519 signature"
  D3: "PDF attachment"
}

E: "5. Acknowledge\nSigned receipt" {
  E1: "applicationId"
  E2: "status"
  E3: "employer signature"
}

Candidate -> A: "DNS lookup"
A -> Candidate: "endpoint + key"
Candidate -> B: "HTTP GET"
B -> Candidate: "JobPosting"
Candidate -> C: "evaluate"
C -> Candidate: "GO/NO-GO"
Candidate -> D: "POST /apply"
D -> Employer: "multipart"
Employer -> E: "process"
E -> Candidate: "acknowledgment"
```

The cryptography uses **Ed25519** (Java `java.security`) with:
- Every application signed by the candidate's private key
- Every attachment has its own SHA-256 digest + signature
- Employers publish their public key in DNS TXT records
- Signed acknowledgments from employers with application IDs

### 5. Daemon MCP Server

The Daemon is a **Model Context Protocol (MCP) server** — a personal API for the headhunter-agent system. It implements JSON-RPC 2.0 over HTTP using the babashka HTTP server:

```d2
direction: down

Client: "MCP Client\n(curl, Claude Code,\nOpenCode, etc.)"

Daemon: "Daemon MCP Server\n:8081" {
  T1: "get_about"
  T2: "get_architecture"
  T3: "get_modules"
  T4: "get_m2m_spec"
  T5: "get_config"
  T6: "get_usage"
  T7: "get_data_contract"
  T8: "get_projects"
  T9: "get_all"
}

Data: "data.clj\nTool content repository"

Client -> Daemon: "tools/list"
Client -> Daemon: "tools/call {name: get_*}"
Daemon -> Data: "fetch content"
Daemon -> Client: "JSON-RPC 2.0 response"
```

Query the daemon with any MCP-compatible client:

```bash
curl -X POST http://localhost:8081 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/list","id":1}'
```

```bash
curl -X POST http://localhost:8081 \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_architecture"},"id":2}'
```

### 6. PDF Resume Pipeline

Resumes are compiled through a multi-stage pipeline:

```d2
direction: down

CV: "cv.md"
JD: "Job Description"
Gemini2: "Gemini API\n(temp: 0.2)"

JSON: "resume_data.json\nATS-tailored JSON"

Typst: "Typst Compiler"

PDF: "output/cv-company-*.pdf"

CV -> Gemini2: "content"
JD -> Gemini2: "requirements"
Gemini2 -> JSON: "responseMimeType: json"
JSON -> Typst: "data input"
Typst -> PDF: "compile resume.typ"
```

### 7. Application Pipeline Tracker

All applications are tracked in a markdown file at `data/applications.md`:

```d2
direction: down

Eval: "evaluator.clj"
PDF2: "pdf.clj"
M2M2: "m2m/submit.clj"

Tracker: "data/applications.md" {
  F1: "ID | Date | Company | Role"
  F2: "Score | Status | PDF | Notes"
}

Eval -> Tracker: "add evaluation entry"
PDF2 -> Tracker: "mark PDF ready"
M2M2 -> Tracker: "add M2M submission"
```

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Language** | Clojure 1.12 | Core logic, all modules |
| **Runtime** | JVM + Babashka | Desktop GUI + CLI automation |
| **GUI** | cljfx (JavaFX) | Native desktop window |
| **AI** | Google Gemini API | All agent intelligence |
| **PDF** | Typst | Resume compilation |
| **JSON** | Cheshire 5.13 | JSON parsing/generation |
| **HTTP** | babashka http-client 0.4.23 | API calls + M2M fetch/submit |
| **Server** | babashka http-server 0.1.7 | Daemon MCP + Directory service |
| **Crypto** | Ed25519 (java.security) | M2M signing + verification |
| **Discovery** | DNS TXT + Java InitialDirContext | M2M endpoint discovery |
| **Data** | .edn files, Markdown | Local storage |

## Key Design Decisions

1. **Privacy-first** — All data stays local as `.edn` / `.md` files. No cloud, no analytics, no telemetry.
2. **No web stack** — Native JavaFX desktop GUI (cljfx). No browsers, no JS, no npm.
3. **Dual interface** — Full GUI desktop app + terminal CLI for automation. Same source code in both.
4. **Babashka compatible** — Same source runs in JVM Clojure and Babashka native binary.
5. **Gemini-powered** — All ML through Google Gemini API (model-agnostic — swap to any provider).
6. **Markdown-based tracker** — Simple, human-readable, git-friendly application database.
7. **Typst for PDF** — Modern typesetting replacing LaTeX / HTML-to-PDF. Fast, deterministic.
8. **Ed25519 for M2M** — Modern post-quantum-ready cryptography for decentralized identity.
9. **Data Contract** — Two-layer file ownership model. User layer is NEVER touched by updates.
10. **MCP protocol** — Daemon exposes the system's knowledge through a standard interface consumable by any AI agent.

## Repository Structure

```
headhunter-agent/
├── src/career_ops/
│   ├── core.clj              CLI entry point & command routing
│   ├── gui.clj               JavaFX desktop GUI (878 lines)
│   ├── profiler.clj          Data Vault extraction from raw text
│   ├── evaluator.clj         3-stage MAS pipeline
│   ├── interview.clj         STAR story interview prep
│   ├── pdf.clj               ATS-tailored PDF resume generator
│   ├── tracker.clj           Application pipeline tracker
│   ├── style.css             Nord Dark GUI theme
│   ├── m2m/                  M2M Protocol module
│   │   ├── core.clj          CLI routing
│   │   ├── crypto.clj        Ed25519 keygen, sign, verify
│   │   ├── schema.clj        JSON-LD validation
│   │   ├── registry.clj      DNS + directory discovery
│   │   ├── directory.clj     Directory server (optional)
│   │   ├── fetch.clj         Job posting HTTP fetcher
│   │   ├── submit.clj        Signed application builder
│   │   └── verify.clj        Inbound signature verification
│   └── daemon/
│       ├── core.clj          MCP server entry point
│       └── data.clj          Tool content repository
├── config/profile.example.yml
├── modes/
│   ├── _shared.md            Scoring system, rules, archetypes
│   ├── _profile.example.md   User archetypes, narrative
│   └── oferta.md             A-G evaluation mode instructions
├── docs/m2m-protocol/
│   ├── SPECIFICATION.md      Full protocol spec (522 lines)
│   └── ARCHITECTURE.md       M2M system architecture
├── deps.edn                  Clojure dependencies
├── bb.edn                    Babashka task definitions
├── resume.typ                Typst resume entry point
├── resume_template.typ       Typst layout template
├── DATA_CONTRACT.md          User vs System layer ownership
└── setup.sh / setup.bat      Platform setup scripts
```

## Getting Started

```bash
# Prerequisites: JDK 11+, Clojure CLI, Babashka (optional)
git clone https://gitlab.com/nurazhar/headhunter-agent.git
cd headhunter-agent

# Setup
cp .env.example .env   # Add your GEMINI_API_KEY
bash setup.sh          # Creates config files from examples

# Launch desktop GUI
clj -M:run

# Or use CLI
bb profile --extract /path/to/linkedin-dump.txt
bb evaluate --file ./jds/sample-jd.txt
bb interview --file ./jds/sample-jd.txt
bb pdf --file ./jds/sample-jd.txt
bb tracker list

# Start Daemon MCP server
bb daemon serve --port 8081

# M2M protocol (experimental)
bb bb-m2m keygen
bb bb-m2m discover employer.example
bb bb-m2m fetch https://employer.example/jobs/42
```

## What's Next

The priority items before production readiness:

1. **Test suite** — 2,400+ lines of Clojure with zero test coverage is the critical gap
2. **CI test runner** — GitLab CI pipeline with automated testing
3. **Versioning** — Semantic versioning with release tags
4. **Linter** — clj-kondo integration for consistent code style
5. **Binary attachments** — Fix the placeholder binary data in M2M submit.clj
6. **Employer-side verification library** — P5 of M2M roadmap

The full source is at **[gitlab.com/nurazhar/headhunter-agent](https://gitlab.com/nurazhar/headhunter-agent)**. The Daemon MCP server runs on `bb daemon serve` and exposes all system documentation through the Model Context Protocol — consumable by any MCP-compatible AI agent.
