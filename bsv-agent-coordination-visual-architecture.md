Title: The BSV Agent Coordination Layer: A Visual Architecture
Date: 2026-06-20
Tags: bsv, ai-agents, architecture, brc105, escrow, op_return, machine-to-machine, clojure, diagrams

---

The previous posts covered *why* AI agents need BSV-native identity and *how* we built live testnet transactions. This post steps up one level: the full system architecture, captured in diagrams.

The BSV Agent Coordination Layer is a Clojure monorepo that gives AI agents economic agency — identity, payments, escrow, audit, policy enforcement — all on Bitcoin SV primitives. 99 tests, 246 assertions, 9 modules, 0 failures.

## System Architecture

The stack has six layers, from the Agent SDK down to the BSV blockchain:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    subgraph Agent["Agent Ecosystem"]
        A1["Alice Agent\nconsumer"]
        A2["Bob Agent\nprovider"]
    end
    subgraph SDK["Agent SDK Layer"]
        SDK1["AgentRuntime\nservice registry"]
        SDK2["AgentMemory\nAnchorChain pattern"]
        SDK3["call-service\npay-agent"]
    end
    subgraph Identity["Identity and Wallet"]
        I1["IAgentIdentity\nsecp256k1 ECDSA"]
        W1["IAgentWallet\natomic balance"]
    end
    subgraph Payments["BRC-105 Payment Layer"]
        P1["402 Challenge\nservice to client"]
        P2["Policy Engine\nbudget, domain, rate"]
        P3["Payment Header\nclient to service"]
    end
    subgraph Audit["Audit and Anchoring"]
        AU1["Event Bundles\nSHA-256 hashing"]
        AU2["OP_RETURN Script\nBSV blockchain"]
        AU3["Chain Verification"]
    end
    subgraph Coordination["Coordination Primitives"]
        C1["Task Commitment\nsigned agreements"]
        C2["Milestone Escrow\nstaged payments"]
        C3["Dispute Resolution\narbitration"]
    end
    subgraph Blockchain["Bitcoin SV (testnet)"]
        BSV["WhatsonChain Faucet\nBitails API\nOP_RETURN TX"]
    end
    Agent --> SDK
    SDK --> Identity
    SDK --> Payments
    SDK --> Audit
    SDK --> Coordination
    Payments --> Identity
    Audit --> Identity
    Coordination --> Identity
    Coordination --> W1
    Payments -->|"402 + x-bsv-payment"| Blockchain
    Audit -->|"anchor hash"| Blockchain
```

Every module is independently testable with acyclic dependencies. The key design rule: identity is the root. Everything — payments, audit, coordination — starts from the agent's secp256k1 keypair.

## BRC-105 Payment Flow

HTTP 402 Payment Required is the handshake protocol. A service returns 402 with challenge headers; the client evaluates policy, reserves funds, constructs a payment proof, and retries:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
sequenceDiagram
    Client->>Service: HTTP GET /translate
    Service-->>Client: 402 Payment Required<br/>x-bsv-payment-version<br/>x-bsv-payment-derivation-prefix
    Client->>Policy: evaluate-payment(500 sats, api.testnet)
    Policy-->>Client: allow
    Client->>Wallet: reserve-funds(500 sats)
    Wallet-->>Client: ok
    Client->>Client: construct-payment-header()
    Client->>Service: HTTP GET /translate<br/>x-bsv-payment: {tx, proof, merklePath}
    Service->>Service: verify-payment-header()
    Service-->>Client: 200 OK + translation result
    Service-->>Client: x-bsv-receipt: {txid, amount}
```

The policy engine sits between the agent's wallet and every payment decision. It enforces budgets, rate limits, domain whitelists, and approval thresholds — all programmatically, with human-only escalation above configurable thresholds.

## Micropayment Escrow and Milestone Flow

For multi-step agent tasks, the coordination module provides milestone escrows. Funds are locked and released incrementally as deliverables are confirmed:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
sequenceDiagram
    Alice->>Escrow: create-escrow(alice, bob, 7000 sats)
    Alice->>Bob: assign task
    Bob-->>Alice: complete
    Alice->>Escrow: complete-milestone(1)
    Escrow-->>Bob: release 2000 sats
    Bob-->>Alice: complete
    Alice->>Escrow: complete-milestone(2)
    Escrow-->>Bob: release 4000 sats
    Bob-->>Alice: complete
    Alice->>Escrow: complete-milestone(3)
    Escrow-->>Bob: release 1000 sats
    Alice->>Escrow: raise-dispute(escrow, quality issue)
    Escrow->>Escrow: resolve-dispute(split, 50%)
    Escrow-->>Alice: return 3500 sats
    Escrow-->>Bob: release 3500 sats
```

The dispute resolution state machine supports three outcomes: release to beneficiary, return to depositor, or split at configurable ratio. Funds are frozen from the moment a dispute is raised.

## Audit Anchoring

Every agent action — payments, task completions, decisions — is bundled into a SHA-256 hash and optionally anchored to BSV via OP_RETURN:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    Blockchain["BSV Blockchain\nimmutable timestamp"]
    Bundle["Audit Bundle\ntype, version, agent,\ntimestamp, data"]
    Event["Agent Event\npayment, task, decision"]
    Hash["SHA-256 Hash\n32 bytes"]
    Script["OP_RETURN Script\n0x6a + len + data"]
    TX["Anchor TX\nversion 1, 1 input,\n1 OP_RETURN output"]
    Verify["Chain Verification\nverify hash integrity"]
    Bundle --> Hash
    Hash --> Script
    Script --> TX
    TX --> Blockchain
    Blockchain --> Verify
```

The `bundle-and-anchor` function accepts an optional wallet. With a wallet, it broadcasts to BSV testnet. Without one, it falls back to local simulation — making the entire test suite run offline.

## Core Domain Model

The domain model is expressed as Clojure records with typed fields:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    subgraph class["PaymentIntent"]
        + String endpoint["+ String endpoint"]
        + Int price_satoshis["+ Int price_satoshis"]
        + String nonce["+ String nonce"]
        + Int expiry["+ Int expiry"]
        + String policy_scope["+ String policy_scope"]
    end
    subgraph class["TaskCommitment"]
        + String task_id["+ String task_id"]
        + String requester["+ String requester"]
        + String provider["+ String provider"]
        + String service["+ String service"]
        + Int max_price["+ Int max_price"]
        + String deadline["+ String deadline"]
        + String signature["+ String signature"]
    end
    subgraph class["PaymentReceipt"]
        + String txid["+ String txid"]
        + Int amount["+ Int amount"]
        + String session_id["+ String session_id"]
        + String timestamp["+ String timestamp"]
        + String signature["+ String signature"]
    end
    subgraph class["AuditAnchor"]
        + String hash["+ String hash"]
        + String bundle_json["+ String bundle_json"]
        + String txid["+ String txid"]
        + Int timestamp["+ Int timestamp"]
        + String status["+ String status"]
    end
    subgraph class["BudgetPolicy"]
        + String policy_id["+ String policy_id"]
        + String agent_id["+ String agent_id"]
        + Int max_satoshis["+ Int max_satoshis"]
        + Int window_seconds["+ Int window_seconds"]
        + List String allowed_domains["+ List String allowed_domains"]
        + Int approval_threshold["+ Int approval_threshold"]
    end
    subgraph class["EscrowAccount"]
        + String escrow_id["+ String escrow_id"]
        + String depositor["+ String depositor"]
        + String beneficiary["+ String beneficiary"]
        + Int total_amount["+ Int total_amount"]
        + List~Milestone~ milestones["+ List~Milestone~ milestones"]
        + String status["+ String status"]
        + Int released["+ Int released"]
    end
    subgraph class["Milestone"]
        + String milestone_id["+ String milestone_id"]
        + String description["+ String description"]
        + Int amount_satoshis["+ Int amount_satoshis"]
        + String status["+ String status"]
    end
    PaymentIntent --> TaskCommitment
    TaskCommitment --> PaymentReceipt
    PaymentReceipt --> AuditAnchor
    BudgetPolicy --> PaymentIntent
    EscrowAccount --> Milestone
    EscrowAccount --> TaskCommitment
```

## Business Model

The architecture enables six economic primitives for autonomous agent commerce:

- **Pay-per-call AI services** — agents monetize with microtransactions (500–10,000 sats), no subscriptions or API keys
- **Staged milestone escrows** — large tasks broken into funded milestones, payment releases on delivery
- **Immutable audit trail** — every action hashed and optionally anchored to BSV via OP_RETURN
- **Programmable policy engine** — budgets, rate limits, domain whitelists enforced automatically
- **BSV-native identity** — secp256k1 keypair for payments, commitments, and audit signatures
- **Open protocol stack** — built on BRC-105, OP_RETURN, secp256k1; interoperable with any BRC-105-compliant service

## Testnet Integration

The full end-to-end flow runs on BSV testnet via the Bitails API:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    Addr["Derive BSV testnet addresses\nAlice: m...\nBob: n..."]
    AgentA["Create Alice Agent\nsecp256k1 identity\n100,000 sats wallet"]
    AgentB["Create Bob Agent\nsecp256k1 identity\n0 sats wallet"]
    AuditGen["Generate audit bundle\nSHA-256 hash + sign"]
    Bal["Check testnet balance\nvia Bitails API"]
    CheckPolicy["Verify policy engine\nallow 500, deny 5000"]
    Escrow["Create milestone escrow\nAlice to 7000 sats to escrow"]
    Milestones["Complete 3 milestones\nResearch, Implement, Verify"]
    ORETURN["Construct OP_RETURN TX\nfor blockchain anchoring"]
    Pay["Execute BRC-105 payment\nAlice pays Bob 500 sats"]
    Policy["Apply spending policies\nbudget: 50k sats/1h\ndomain allow: bob-testnet"]
    Reg["Register BRC-105 service\nBob: translate 500 sats/call"]
    Release["Release payment to Bob"]
    subgraph Report["Print summary report\naddresses, balances, status"]
    end
    VerifyAudit["Verify audit chain integrity"]
    AgentA --> AgentB
    AgentA --> Addr
    Addr --> Bal
    Bal --> Reg
    Reg --> Policy
    Policy --> Pay
    Pay --> AuditGen
    AuditGen --> ORETURN
    ORETURN --> Escrow
    Escrow --> Milestones
    Milestones --> Release
    Release --> CheckPolicy
    CheckPolicy --> VerifyAudit
    VerifyAudit --> Report
```

## Stack Summary

| Layer | Technology | BSV Standard |
|-------|-----------|-------------|
| Identity | Bouncy Castle secp256k1 | ECDSA (same curve as BSV) |
| Payments | BRC-105 HTTP 402 | [BRC-105](https://bsv.brc.dev/payments/0105) |
| Anchoring | OP_RETURN + SHA-256 | Bitcoin Script OP_RETURN |
| Testnet API | Bitails API | — |
| Runtime | Clojure on JVM | — |

The full architecture page with interactive diagrams, all 9 module diagrams, and the complete business model breakdown is available in the repository's [architecture.html](https://github.com/nurazhardotcom/bsv-agent-coordination).

99 tests. 246 assertions. 0 failures. 9 modules. One coordination layer for sovereign agent commerce.
