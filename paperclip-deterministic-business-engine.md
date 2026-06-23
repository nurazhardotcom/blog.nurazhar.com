Title: The Paperclip Engine: A Deterministic Business Kernel in Clojure
Date: 2026-06-21
Tags: clojure, bsv, governance, policy, automation, income, engine, solo-founder, brc105

---

The previous posts covered the BSV coordination layer (the glue between agents and Bitcoin primitives) and its architecture. This post covers something different: a complete business engine.

Paperclip-clj is a deterministic Clojure kernel for autonomous business operations. It integrates governance, policy enforcement, billing, escrow, and blockchain settlement into one acyclic module graph. 33 tests, 54 assertions, 0 failures, 0 errors.

## The Engine vs The Glue

The BSV Agent Coordination Layer was glue: it connected AI agents to BSV testnet primitives (identity, payments, OP_RETURN anchoring). Paperclip is an engine: a self-contained business runtime with its own domain model, policy engine, billing system, and execution pipeline. BSV is one adapter among many — the engine would work just as well with Stripe or PayPal.

## System Architecture

The engine is organized into five layers:

```d2
# Diagram 141
vars: {
  d2-config: {
    theme-id: 200
  }
}

Core: "Core Domain" {
  CORE: "paperclip.core\nproposals, organizations, voting"
  GOV: "paperclip.governance\nexecute approved proposals"
  LEDGER: "paperclip.ledger\nappend-only audit trail"
}

Policy: "Policy Layer" {
  PLAY: "paperclip.playbook\napproved task schemas"
  POL: "paperclip.policy\nbudget caps, risk tiers, auto-approve"
  ROUTE: "paperclip.routing\ngreen, yellow, red lanes"
}

Billing: "Billing Layer" {
  BILL: "paperclip.billing\nfee calculation, extraction math"
  COMM: "paperclip.commercial\nprepaid escrow gate"
}

Execution: "Execution Layer" {
  ATT: "paperclip.attention\nmindshare scoring"
  FALLBACK: "paperclip.fallback\nstatic safe states"
  REPAIR: "paperclip.repair\nbounded self-healing"
}

Adapters: "Adapters" {
  AGENT: "adapters.agent\nLLM sandbox"
  BSV: "adapters.bsv\nSHA-256 + OP_RETURN"
  EMAIL: "adapters.email"
  WEBHK: "adapters.webhook"
}

Core -> Policy
Policy -> Billing
Billing -> Execution
Execution -> Adapters
```

Every namespace is pure Clojure with no mutable state at the core. Side effects are isolated in the adapter layer.

## The Five Gates

The engine enforces five fail-closed gates before any work executes, as specified in the original design:

| Gate | Module | Question | If failed |
|------|--------|----------|-----------|
| Payment | commercial.clj | Is money already deposited? | No execution |
| Scope | playbook.clj | Is this request in an approved contract? | Dead-letter drop |
| Input | playbook.clj | Does payload match allowed schema? | Dead-letter drop |
| Policy | policy.clj | Is spend within cap, vendor approved? | Rejected |
| Attention | attention.clj | Will this task waste founder focus? | Bounce to agent |

Only tasks that pass all five gates reach the BSV settlement adapter.

## Execution Pipeline

The routing engine maps every task into one of four lanes:

```d2
# Diagram 142
vars: {
  d2-config: {
    theme-id: 200
  }
}

DL: "dead-letter queue"
EXEC: "silent execution\nauto-approve"
PLAYBOOK: "playbook.clj\nvalidate contract"
REPAIR: "repair.clj\n3 retry attempts"
ROUTE: "routing.clj\ndetermine lane"
SETTLE: "BSV settlement\nfounder authorized"
TASK: "Task Input"

PLAYBOOK -> ROUTE: "valid"
PLAYBOOK -> DL: "invalid"
ROUTE -> EXEC: "green"
ROUTE -> REPAIR: "yellow"
ROUTE -> SETTLE: "red"
ROUTE -> DL: "dead-letter"
REPAIR -> EXEC: "success"
REPAIR -> DL: "exhausted"
```

The green lane runs without any human attention. The yellow lane attempts bounded self-healing (3 retries max, no LLM repair loop). The red lane triggers BSV settlement with split payouts. Everything else drops silently into the dead-letter queue.

## Billing and Income Extraction

The billing engine computes fees at three levels:

```clojure
(def default-pricing
  {:pricing/workspace-monthly 499.00
   :pricing/execution-fee 0.25
   :pricing/budget-unlock-rate 0.015
   :pricing/settlement-rate 0.010
   :pricing/min-settlement-fee 1.00
   :pricing/anchor-fee-bsv 0.000005})

(defn calculate-fees [{:keys [proposal execution-count settlement-amount pricing]}]
  ;; Returns workspace-monthly, execution-fee, unlock-fee,
  ;; settlement-fee, anchor-fee, and total-variable-fee
  ...)
```

Revenue comes from three streams: monthly workspace retainers, usage-based execution fees, and optional BSV settlement margin. The commercial module enforces prepaid escrow — no balance means no execution.

## BSV Integration

The BSV adapter generates deterministic SHA-256 digests, constructs split-payout transactions (vendor + platform fee), and anchors OP_RETURN proofs:

```clojure
(defn settle-payout-with-extraction!
  [{:keys [vendor-address platform-treasury-address]} proposal billing-breakdown]
  (let [receipt-hash (canonicalize-and-hash proposal billing-breakdown)
        tx-template {:outputs [{:address vendor-address
                                :satoshis (to-sats net-vendor-payout)}
                               {:address platform-treasury-address
                                :satoshis (to-sats platform-fee)}
                               {:op-return receipt-hash}]}]
    ...))
```

## Revenue Model

The engine is designed for a solo founder targeting $100,000/year through:

1. **One narrow workflow** — pick one painful business process, encode it as a playbook
2. **Two B2B retainers** at $4,500/month each = $108,000/year ARR
3. **Prepaid escrow** — no credit, no net-30, no invoices
4. **Dead-letter as contract law** — dropped invalid inputs are defined as correct system behavior, not support tickets

## Test Results

All 33 tests pass with 54 assertions:

| Module | Tests | What it proves |
|--------|-------|----------------|
| core | 8 | Proposal lifecycle, voting, quorum evaluation |
| governance | 2 | Approved/non-approved proposal execution |
| policy | 10 | Spending limits, risk tiers, agent intent, founder override |
| billing | 3 | Fee calculation, extraction model, pricing |
| playbook | 6 | Contract validation, schema enforcement, immutable contracts |
| attention | 4 | Risk scoring, triage lanes, escalation thresholds |

The engine is at [github.com/nurazhardotcom/paperclip-clj](https://github.com/nurazhardotcom/paperclip-clj). Next step: deploy for one paying client and prove the loop closes.
