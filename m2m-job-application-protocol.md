Title: Designing a Machine-to-Machine Job Application Protocol
Date: 2026-06-24
Tags: clojure, babashka, m2m, protocol, systems-design, cryptography, json-ld
Description: A deep dive into the M2M Job Application Protocol — eliminating browsers, CAPTCHAs, and manual portals from the recruiting pipeline using JSON-LD schemas, decentralized discovery via DNS, Ed25519 cryptographic identity, and signed application packages built with Clojure and Babashka.

---

## The Problem

Job applications today are a manual, browser-bound chore. You fill out the same fields a hundred times, solve CAPTCHAs that prove you're human (but waste your time), upload the same PDF to every portal, and wait. The process assumes a human on both sides.

But what if neither side needs to be human at the application stage?

The **headhunter-agent** project already eliminates the browser on the candidate side: it's a native Clojure/JavaFX desktop app that evaluates job postings, profiles candidates, and generates tailored PDFs — all without a web browser. The missing piece was an employer-facing protocol that closes the loop: machine-readable job postings, automated discovery, and signed application submission.

This post describes the M2M Job Application Protocol v1.0.0: a full-stack protocol for machine-to-machine recruiting, designed as an extension to headhunter-agent.

## System Context

The protocol connects four actors:

```d2
direction: down

Candidate: Candidate Agent {
  shape: hexagon
  style.multiple: true
}
Employer: Employer System {
  shape: hexagon
  style.multiple: true
}
Registry: Registry / DNS

Candidate -> Registry: 1. Discover endpoint
Candidate -> Employer: 2. Fetch job posting
Candidate -> Employer: 3. Submit signed application
Employer -> Candidate: 4. Signed acknowledgment

Registry -> Employer: DNS TXT publish
```

## The Four Sub-Protocols

The M2M protocol decomposes into four sub-protocols that execute in sequence:

```d2
direction: right

Discovery: Discovery {
  shape: diamond
}
Fetch: Fetch {
  shape: diamond
}
Verify: Verify {
  shape: diamond
}
Submit: Submit {
  shape: diamond
}

Discovery -> Fetch: endpoint + public key
Fetch -> Verify: job posting (JSON-LD)
Verify -> Submit: verified posting
Submit: application sent
```

### 1. Discovery — DNS TXT Records

No central registry. No API keys. No sign-ups.

Employers publish a single DNS TXT record on their domain:

```d2
direction: down

DNS: DNS Server {
  shape: cylinder
}
TXT: _m2m-apply.employer.com TXT {
  shape: code
  style.font: mono
}
Candidate: Candidate Agent {
  shape: hexagon
}

Candidate -> DNS: dig TXT _m2m-apply.employer.com
DNS -> TXT: lookup
TXT -> Candidate: 'm2m-p1;https://.../m2m/v1;key=MCow...'
Candidate -> Candidate: parse endpoint + public key
```

The TXT record format is intentionally simple:

```d2
direction: right
Record: TXT Record {
  shape: text
  style.font: mono
  label: |md
    `m2m-p1;https://employer.com/m2m/v1;key=MCowBQYD...`
  |
}
Parts: {
  Version: m2m-p1
  Endpoint: https://employer.com/m2m/v1
  Key: MCowBQYD...
}
Record -> Parts: splits into
```

A Babashka fallback directory service aggregates known endpoints for discovery when DNS is unavailable:

```d2
direction: down
Candidate: Candidate Agent
Directory: Directory Service {
  shape: document
}
PostgreSQL: PostgreSQL {
  shape: cylinder
}

Candidate -> Directory: GET /v1/search?q=platform+engineer
Directory -> PostgreSQL: query
PostgreSQL -> Directory: results
Directory -> Candidate: 'results [{domain, endpoint, key}]'
```

### 2. Fetch — Machine-Readable Job Postings

Employers serve job postings as JSON-LD over HTTPS. The schema extends schema.org/JobPosting with M2M-specific fields for protocol metadata:

```d2
direction: right

Employer: Employer Server
Candidate: Candidate Agent
Posting: Job Posting (JSON-LD) {
  shape: code
  style.font: mono
}

Candidate -> Employer: GET /m2m/v1/jobs
Employer -> Posting: serve JSON-LD
Posting -> Candidate: 'validates posting'
Candidate -> Candidate: validate schema
```

The JSON-LD posting carries everything a machine needs: title, description, skills, compensation range, and crucially, the application endpoint and employer public key.

```d2
direction: down
Posting: JSON-LD Structure {
  shape: code
  style.font: mono
  label: |md
    ```json
    {
      \"@type\": \"m2m:JobPosting\",
      \"schema:title\": \"Senior Platform Eng\",
      \"schema:skills\": [\"K8s\",\"Terraform\"],
      \"m2m:applyEndpoint\": \"https://.../apply\",
      \"m2m:publicKey\": \"MCowBQYD...\",
      \"m2m:compensation\": {
        \"schema:currency\": \"SGD\",
        \"schema:minValue\": 120000
      }
    }
    ```
  |
}
Blocks: {
  Core: schema.org fields {
    shape: rectangle
  }
  M2M: Protocol fields {
    shape: circle
    style.stroke-dash: 3
  }
}
Posting -> Blocks: contains
```

### 3. Verify — Cryptographic Handshake

Every participant generates an Ed25519 keypair. No CA hierarchy, no certificate authorities. Trust is established through DNS binding:

```d2
direction: right
Candidate: Candidate Agent {
  shape: hexagon
}
BB: bb m2m keygen {
  shape: code
}

BB -> Candidate: Ed25519 keypair
Candidate.key: Public key {
  shape: code
  style.font: mono
}
Candidate.priv: Private key {
  shape: code
  style.font: mono
  style.fill: red
}

key: Public Key
key -> Employer: embedded in every application
```

Applications are signed at two levels:

```d2
direction: down

Package: Application Package {
  shape: code
  style.font: mono
  label: |md
    ```
    {
      \"m2m:candidate\": {...},
      \"m2m:attachments\": [
        {
          \"filename\": \"cv.pdf\",
          \"digest\": \"sha256:abc...\",
          \"digestSignature\": \"MEYCIQ...\"
        }
      ],
      \"m2m:signature\": {
        \"signedPayload\": \"MEUCIQ...\",
        \"algorithm\": \"Ed25519\"
      }
    }
    ```
  |
}
Signature: Envelope Signature {
  shape: circle
  style.stroke-dash: 3
}
Attachment: Attachment Signature {
  shape: circle
  style.stroke-dash: 3
}

Package -> Signature: signed by candidate
Package -> Attachment: per-file
```

1. **Envelope signature**: canonical JSON of the entire package (minus the signature field) is signed with Ed25519
2. **Attachment signature**: each file's SHA-256 digest is independently signed, enabling individual file verification

```d2
direction: down
signing: Signing Pipeline
Source: {
  JSON: Canonical JSON
  PDF: Resume PDF
  digest: SHA-256
}
crypto: Ed25519 {
  shape: hexagon
  style.font: mono
}
priv: Private Key {
  shape: code
  style.fill: red
}

JSON -> crypto: sign canonical JSON
PDF -> digest: hash
digest -> crypto: sign digest
crypto: signedPayload + digestSignature
priv -> crypto
```

### 4. Submit — Signed Application Delivery

The signed package is delivered as a multipart HTTP POST:

```d2
direction: down
Candidate: Candidate Agent
Employer: Employer Server {
  shape: hexagon
}
multipart: Multipart POST /m2m/v1/apply {
  shape: code
  style.font: mono
  label: |md
    ```
    POST /m2m/v1/apply
    Content-Type: multipart/form-data
    
    --boundary
    Content-Type: application/ld+json
    
    { \"@type\": \"m2m:ApplicationPackage\", ... }
    
    --boundary
    Content-Type: application/pdf
    M2M-Digest: sha256:abc...
    M2M-Digest-Signature: MEYCIQ...
    
    [binary PDF]
    --boundary--
    ```
  |
}

Candidate -> Employer: POST signed package
Employer -> Employer: verify signature
Employer -> Candidate: signed receipt
```

The employer responds with a signed acknowledgment:

```d2
direction: right

Ack: Acknowledgment {
  shape: code
  style.font: mono
  label: |md
    ```json
    {
      \"@type\": \"m2m:Acknowledgment\",
      \"status\": \"accepted\",
      \"applicationId\": \"APP-2026-06-24-0042\",
      \"receivedAt\": \"2026-06-24T12:00:05+08:00\",
      \"signature\": {
        \"signedPayload\": \"...\",
        \"algorithm\": \"Ed25519\"
      }
    }
    ```
  |
}

Candidate -> Candidate: verify ack signature
```

## Full Data Flow

Here's the complete flow from discovery to submission:

```d2
direction: down

Candidate: headhunter-agent
DNS: DNS Server
Employer: Employer M2M Server

Candidate -> DNS: dig TXT _m2m-apply.employer.com
DNS -> Candidate: endpoint + public key

Candidate -> Employer: GET /m2m/v1/jobs/42
Employer -> Candidate: JSON-LD JobPosting

Candidate -> Candidate: Evaluate (MAS pipeline)
Candidate -> Candidate: Tailor resume (Typst)
Candidate -> Candidate: Sign package (Ed25519)

Candidate -> Employer: POST /m2m/v1/apply (signed multipart)
Employer -> Employer: Verify signature
Employer -> Candidate: Signed Acknowledgment
```

## Protocol Stack

```d2
direction: down

Application: Application Layer {
  shape: code
  label: |md
    **Application Layer**
    Application Package, Acknowledgment
    JSON-LD + Ed25519 signatures
  |
}
Presentation: Presentation Layer {
  shape: code
  label: |md
    **Presentation Layer**
    Canonical JSON, SHA-256 digests
    Base64 encoding, JSON Schema validation
  |
}
Session: Session Layer {
  shape: code
  label: |md
    **Session Layer**
    M2M Handshake: key exchange, nonce
    Token-less, signature-based auth
  |
}
Transport: Transport Layer {
  shape: code
  label: |md
    **Transport Layer**
    HTTPS (TLS 1.3 mandatory)
    DNS UDP/TCP for discovery
  |
}
Network: Network Layer {
  shape: code
  label: |md
    **Network Layer**
    IPv4/IPv6
  |
}
Application -> Presentation
Presentation -> Session
Session -> Transport
Transport -> Network
```

## Application Lifecycle State Machine

```d2
direction: right

DISCOVER: DISCOVER {
  shape: hexagon
}
FETCHED: FETCHED
EVALUATED: EVALUATED
NOGO: NO-GO {
  shape: hexagon
  style.fill: "#d32f2f"
}
TAILORED: TAILORED
SIGNED: SIGNED
SUBMITTED: SUBMITTED
ACKED: ACKNOWLEDGED
REVIEW: IN REVIEW {
  shape: circle
  style.stroke-dash: 3
}
RESOLVED: RESOLVED

DISCOVER -> FETCHED
FETCHED -> EVALUATED
EVALUATED -> NOGO: skip
EVALUATED -> TAILORED: GO
TAILORED -> SIGNED
SIGNED -> SUBMITTED
SUBMITTED -> ACKED
ACKED -> REVIEW
REVIEW -> RESOLVED: offer / rejection
NOGO -> DISCOVER: retry
```

## Implementation Architecture

The protocol is implemented as eight Clojure namespaces under `career-ops.m2m`, extending the existing headhunter-agent:

```d2
direction: right

m2m: M2M Protocol {
  shape: circle
  style.font: mono
}
existing: headhunter-agent {
  shape: circle
}

core: core.clj (CLI router)
crypto: crypto.clj (Ed25519)
schema: schema.clj (JSON-LD)
registry: registry.clj (DNS)
fetch: fetch.clj (HTTP fetcher)
submit: submit.clj (package builder)
verify: verify.clj (signature check)
directory: directory.clj (server)

core -> crypto
core -> schema
core -> registry
core -> fetch
core -> submit
core -> verify
core -> directory

existing -> m2m: extends
```

Each namespace has a single responsibility:

| Module | File | Lines | Responsibility |
|--------|------|-------|----------------|
| `core` | `core.clj` | 159 | CLI routing for `bb m2m {keygen,discover,fetch,apply,verify,serve}` |
| `crypto` | `crypto.clj` | 133 | Ed25519 key generation, signing, verification, SHA-256 digests, canonical JSON |
| `schema` | `schema.clj` | 106 | JSON-LD @context, structural validation for all message types |
| `registry` | `registry.clj` | 85 | DNS TXT lookup + HTTP directory fallback |
| `fetch` | `fetch.clj` | 66 | Job posting HTTP fetcher with schema validation |
| `submit` | `submit.clj` | 91 | Application package assembly, attachment signing, multipart POST |
| `verify` | `verify.clj` | 82 | Inbound envelope + attachment signature verification |
| `directory` | `directory.clj` | 76 | Optional Babashka HTTP directory aggregator server |

## CLI Usage

The protocol is exposed through a unified CLI:

```d2
direction: down
CLI: bb m2m {
  shape: code
  style.font: mono
  label: |md
    ```
    bb m2m keygen                          # Generate identity
    bb m2m discover <domain>               # Find employer endpoint
    bb m2m fetch <url>                     # Validate job posting
    bb m2m apply <url>                     # Full M2M pipeline
    bb m2m verify <package>                # Check signature
    bb m2m serve                           # Directory server
    ```
  |
}

Phases: {
  Phase0: P0 — crypto + schema
  Phase1: P1 — discovery + fetch
  Phase2: P2 — submit + verify
  Phase3: P3 — directory server
  Phase4: P4 — full integration with MAS
}
CLI -> Phases: built in stages
```

The `bb m2m apply` command chains the entire pipeline:

1. **Discover** — DNS lookup for employer endpoint
2. **Fetch** — HTTP GET for JSON-LD job posting
3. **Evaluate** — Existing 3-stage MAS pipeline (legitimacy, fit, cheat sheet)
4. **Tailor** — Gemini-powered resume tailoring, compiled via Typst
5. **Sign** — Ed25519 signature on package + attachment digests
6. **Submit** — Multipart POST to employer endpoint
7. **Acknowledge** — Verify employer's signed receipt

## Security Model

```d2
direction: right

threats: Threats {
  Impersonation
  Replay
  Tampering
  MITM
  Registry Poisoning
}
mitigations: Mitigations {
  DNS-binding: DNS-bound public keys
  Timestamps: Timestamp + nonce
  Signatures: Ed25519 per-file signatures
  TLS: TLS 1.3 + signed payloads
  Signed: Signed directory entries
}
threats -> mitigations
```

| Threat | Mitigation |
|--------|-----------|
| Employer impersonation | DNS TXT binds public key to domain; TLS validates transport |
| Candidate impersonation | Ed25519 signature on every application envelope |
| Replay attack | Timestamps + server-enforced freshness window |
| Tampered resume | Per-attachment SHA-256 digest + independent Ed25519 signature |
| Man-in-the-middle | TLS 1.3 for all HTTP; signatures provide end-to-end integrity |
| Registry poisoning | Directory entries are signed; primary discovery is DNS (DNSSEC-ready) |

### No-CAPTCHA Guarantee

The protocol explicitly eliminates all human verification:

```d2
direction: down
Portal: Traditional Portal {
  shape: hexagon
  style.fill: "#d32f2f"
}
M2M: M2M Protocol {
  shape: hexagon
  style.fill: "#2e7d32"
}
human: {
  CAPTCHA: CAPTCHA
  Forms: Manual forms
  Login: Portal login
  Email: Email verification
}
block: {
  Signed: Signed identity
  Endpoint: Opt-in endpoints
  Rate: Key-based rate limiting
  Rep: Reputation scoring
}
Portal -> human: requires
M2M -> block: replaces with
Portal -> M2M: eliminates
```

- **Endpoints opt in** by publishing `m2m:acceptsSignedApplications: true`
- **Signed identity** replaces cookies, sessions, and auth walls
- **Rate limiting** is by public key fingerprint, not IP address
- **Reputation** is based on application quality, not manual review

## Protocol Extension Points

```d2
direction: down
Extensions: Extension Points {
  shape: circle
  style.stroke-dash: 3
}
Extension1: Custom screening questions {
  shape: document
}
Extension2: Skills assessments {
  shape: document
}
Extension3: Portfolio attachments {
  shape: document
}
Extension4: Multi-party applications {
  shape: document
}
Extension5: DID integration {
  shape: document
}
Extension6: Encrypted submissions {
  shape: document
}

Extensions -> Extension1: m2m:applicationSchema
Extensions -> Extension2: m2m:assessmentUrl
Extensions -> Extension3: 'extra attachments[]'
Extensions -> Extension4: multiple candidate profiles
Extensions -> Extension5: 'did:key: identifiers'
Extensions -> Extension6: envelope encryption
```

## Integration with headhunter-agent

The protocol extends the existing project without breaking any existing functionality:

```d2
direction: down
existing: headhunter-agent existing {
  Core: core.clj — CLI router
  Profiler: profiler.clj — Data Vault
  Evaluator: evaluator.clj — 3-stage MAS
  PDF: pdf.clj — Resume tailor + Typst
  Tracker: tracker.clj — Markdown DB
  GUI: gui.clj — JavaFX desktop
}
m2m: M2M Protocol (new) {
  shape: circle
  style.stroke-dash: 3
  style.stroke: "#2e7d32"
}

existing -> m2m: extends pipeline
m2m -> Evaluator: chained in bb m2m apply
m2m -> PDF: chained in bb m2m apply
m2m -> Tracker: logs submissions
```

The existing MAS pipeline (Legitimacy check → Fit Analysis → Cheat Sheet) remains unchanged. The M2M module calls it as a step in the apply workflow, then adds signing and submission on top.

## Implementation Roadmap

The protocol is designed for incremental delivery:

```d2
direction: right
P0: P0 — crypto + schema {
  style.stroke: "#2e7d32"
}
P1: P1 — discovery + fetch
P2: P2 — submit + verify
P3: P3 — directory server
P4: P4 — full pipeline integration
P5: P5 — employer-side lib

P0 -> P1
P1 -> P2
P2 -> P3
P3 -> P4
P4 -> P5
```

| Phase | Components | Dependency |
|-------|-----------|------------|
| P0 | `crypto.clj`, `schema.clj`, `core.clj` (keygen + verify) | None |
| P1 | `registry.clj` (DNS), `fetch.clj` | P0 |
| P2 | `submit.clj`, `verify.clj` | P0, P1 |
| P3 | `directory.clj` | P0 |
| P4 | Full `bb m2m apply` with MAS pipeline | P0-P3 |
| P5 | Employer-side verification library | P0, P2 |

## The Bigger Picture

The M2M Job Application Protocol is a small piece of a larger vision: moving all white-collar recruiting from human-mediated workflows to autonomous agent coordination.

The protocol is:

- **Local-first**: all candidate data stays on-device until the moment of submission
- **Privacy-preserving**: no third-party job boards, no data brokers, no resume databases
- **Decentralized**: no central authority required — DNS is the registry
- **Cryptographically verifiable**: every message carries proof of origin and integrity
- **Extension-ready**: the JSON-LD schemas and protocol messages are designed for forward compatibility

The specification and implementation are part of the headhunter-agent project, which is MIT-licensed open source.

For the full protocol specification, including detailed message formats, schema definitions, and the complete Clojure implementation, see the repository at [gitlab.com/nurazhar/headhunter-agent](https://gitlab.com/nurazhar/headhunter-agent).

```d2
direction: down
Future: Future Vision {
  shape: cloud
}
Agents: Recruiting Agents {
  shape: hexagon
  style.multiple: true
}
Human: Human at inflection points {
  shape: person
}

Future -> Agents: negotiate autonomously
Agents -> Human: escalate for decisions
Agents.A: Employer agent posts role
Agents.B: Candidate agent evaluates fit
Agents.C: Both agents negotiate terms
Human: human reviews final offer
```
