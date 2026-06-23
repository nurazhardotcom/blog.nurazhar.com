Title: From Simulation to Settlement: Live Testnet BSV for AI Agents
Date: 2026-06-20
Tags: clojure, bsv, testnet, ai-agents, bitcoin, brc105, op_return, utxo, machine-to-machine

---

The previous post laid out the vision: sovereign AI agents using Clojure + BSV as two primitives that collapse six layers of vendor dependencies into zero. That post ended at "steps 4 and 5 are the frontier — the glue layer that has not been built yet."

Today, that frontier gets paved. This post walks through the live testnet implementation of the BSV Agent Coordination Layer — a Clojure monorepo that turns simulated stubs into real BSV testnet transactions. 100 tests, 242 assertions, zero failures.

## The Gap: What Was Simulated

The original codebase had working coordination logic — identity protocols, atomic wallet reserves, policy engines, escrow workflows — but every interaction with the ledger was mocked:

| Layer | Simulated | Live Replacement |
|-------|-----------|-----------------|
| Address derivation | Hardcoded `"mxx..."` string | secp256k1 pubkey -> RIPEMD160(SHA256) -> Base58Check |
| Transaction spending | `(str "tx-" (random-nonce))` | UTXO selection, P2PKH build, DER signing, broadcast |
| OP_RETURN anchoring | Local SHA-256 hash only | Real OP_RETURN tx with change output |
| Balance | In-memory atom | Bitails API (`get-balance`) |
| UTXOs | Manually set | Bitails API (`get-unspent`) |
| Faucet | No-op | WhatsonChain API + poll-for-confirmation |
| Network calls | No retry | Exponential backoff (3 retries) |

The architecture required six modules with acyclic dependencies:

```d2
# Diagram 80
direction: down

Core: "core: Specs + Primitives" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
  CORE: "CORE" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  COORD: "COORD" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
}

Identity: "wallet: Keys + Addresses" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
  ID: "ID" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  WAL: "WAL" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
}

Payment: "brc105: HTTP 402" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
  BRC: "BRC" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  POL: "POL" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
}

Audit: "audit: OP_RETURN" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
  AUD: "AUD" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
}

SDK: "agent-sdk: Agent Runtime" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}

Testnet: "testnet: Live BSV" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
}

CLI: "CLI" {
  style.fill: "#ffffff"
  style.stroke: "#dee2e6"
}

ID -> WAL
WAL -> CORE
CORE -> COORD
COORD -> AUD
BRC -> POL
BRC -> WAL
SDK -> ID
SDK -> WAL
SDK -> BRC
SDK -> AUD
CLI -> ID
CLI -> WAL
```

The goal: replace the dotted-line simulations with solid, broadcast-ready transactions without changing a single coordination-logic test.

## Step 1: Address Derivation Into the Identity Protocol

The first fix was architectural. Address derivation (pubkey -> Base58Check address) lived in `testnet.client` as a utility. But every agent identity *is* an address — it should be a first-class protocol method.

Before (testnet/client.clj, leaky):
```clojure
(defn agent->address [agent]
  (pubkey->address (identity/public-key (:identity agent))))
```

After (coordination/identity.clj, protocol):
```clojure
(defprotocol IAgentIdentity
  (agent-id [_])
  (public-key [_])
  (sign [_ data])
  (verify [_ data signature])
  (address [_] [_ version-byte]))
```

Every identity now answers `(address identity)` returning `"m..."` or `"n..."` for testnet, `"1..."` for mainnet. The 25-byte Base58Check encoding is self-contained:

```clojure
(defn pubkey->address
  ([pubkey-bytes] (pubkey->address pubkey-bytes 0x6f))
  ([pubkey-bytes version-byte]
   (let [hash160 (ripemd160 (sha256 pubkey-bytes))
         versioned (byte-array (inc (alength hash160)))]
     (aset versioned 0 (byte version-byte))
     ;; ... checksum, encode-base58 ...
     )))
```

The testnet client delegates to identity now, removing the cycle that forced forward-declarations.

```d2
# Diagram 81
direction: down

Before: "Before: Address in testnet client" {
  style.fill: "#f8d7da"
  style.stroke: "#f5c6cb"
  TC1: "testnet.client/pubkey->address" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  TC2: "testnet.client/agent->address" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  TC1 -> TC2
}

After: "After: Address in identity protocol" {
  style.fill: "#d4edda"
  style.stroke: "#c3e6cb"
  AG: "AgentIdentity record" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  ID1: "identity/address (protocol method)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  ID1 -> AG
}

Before -> After: "moved"
```

## Step 2: Live Transaction Building

The core of the work. A valid BSV P2PKH transaction requires:

1. UTXO selection from Bitails API
2. Transaction serialization (version, inputs, outputs, locktime)
3. SIGHASH preimage construction for each input
4. ECDSA signing with SIGHASH_ALL appended
5. Broadcast via Bitails `/tx/broadcast`

### UTXO Selection

Coin selection with fee estimation. The simplest approach: sort UTXOs descending, grab until we cover `amount + fee`:

```clojure
(defn select-utxos [utxos target-amount estimated-fee]
  (let [needed (+ target-amount estimated-fee)
        sorted (sort-by (comp - :satoshis) utxos)
        selected (reduce (fn [acc u]
                           (if (>= (:total acc) needed)
                             (reduced acc)
                             (update acc :selected conj u)
                             (update acc :total + (:satoshis u))))
                         {:selected [] :total 0}
                         sorted)]
    (if (>= (:total selected) needed)
      (assoc selected :change (- (:total selected) target-amount))
      (throw (ex-info "Insufficient UTXOs" {:needed needed})))))
```

### Transaction Serialization

Every byte matters. The BSV wire format is little-endian, with varint prefixes for variable-length fields:

```d2
# Diagram 82
direction: down

Tx: "Raw BSV Transaction" {
  style.fill: "#f8f9fa"
  style.stroke: "#dee2e6"
  I1: "Input 1" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  I2: "Input 2 (optional)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  IC: "varint: Input Count" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  L: "4 bytes: Locktime" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  O1: "Output 1 (payment)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  O2: "Output 2 (change)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  OC: "varint: Output Count" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  V: "4 bytes: Version (1)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
}

Input: "Each Input" {
  style.fill: "#fff3cd"
  style.stroke: "#ffeeba"
  SCR: "ScriptSig (empty before signing)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  SCRL: "varint: ScriptSig Length" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  SEQ: "4 bytes: Sequence (0xffffffff)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  TXID: "32 bytes: Prev TXID (reversed)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  VOUT: "4 bytes: Vout" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
}

Output: "Each Output" {
  style.fill: "#d4edda"
  style.stroke: "#c3e6cb"
  SAT: "8 bytes: Satoshis" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  SCR2: "ScriptPubKey (P2PKH)" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
  SCRL2: "varint: ScriptPubKey Length" {
    style.fill: "#ffffff"
    style.stroke: "#dee2e6"
  }
}

Tx -> Input
Tx -> Output
V -> IC
I1 -> I2
OC -> O1
O2 -> L
TXID -> VOUT
SCRL -> SCR
SAT -> SCRL2
```

The serialization is a straightforward byte-array concatenation:

```clojure
(defn- serialize-tx [tx]
  (let [ver (le32 (:version tx))
        in-count (varint (count (:inputs tx)))
        in-bytes (apply concat (for [in (:inputs tx)]
                                 (concat (hex->bytes (reverse-hex (:txid-hex in)))
                                         (le32 (:vout in))
                                         (varint (count (:script-hex in)))
                                         (hex->bytes (:script-hex in))
                                         (le32 (:sequence in)))))
        out-count (varint (count (:outputs tx)))
        out-bytes (apply concat (for [out (:outputs tx)]
                                  (concat (le64 (:satoshis out))
                                          (varint (count (:script-hex out)))
                                          (hex->bytes (:script-hex out)))))
        locktime (le32 (:locktime tx))]
    (byte-array (concat ver in-count in-bytes
                        out-count out-bytes locktime))))
```

### SIGHASH + Signing

The Bitcoin signing algorithm: double-SHA256 of the sighash preimage, ECDSA sign, append `0x01` (SIGHASH_ALL):

```d2
# Diagram 83
shape: sequence_diagram

TX: "TX"
BUILD: "BUILD"
SIGN: "SIGN"
BROADCAST: "BROADCAST"

TX -> BUILD: "For each input, set script = prevout scriptPubKey"
BUILD -> BUILD: "Serialize to bytes"
BUILD -> BUILD: "Append 0x01000000 (SIGHASH_ALL LE)"
BUILD -> SIGN: "32 bytes: SHA256(SHA256(preimage))"
SIGN -> SIGN: "DER-encode signature"
SIGN -> SIGN: "Append 0x01 byte"
SIGN -> TX: "Build scriptSig: <sig+hashbyte> <pubkey>"
TX -> BROADCAST: "Broadcast raw hex"
BROADCAST -> TX: "Return txid" {
  style.stroke-dash: 5
}
```

The signing function uses the same Bouncy Castle `SHA256withECDSA` that the identity protocol uses — butnote the double-hash:

```clojure
(defn- sign-input [tx input-index prevout-script-hex key-material]
  (let [preimage (sighash-preimage tx input-index prevout-script-hex)
        hash (sha256 (sha256 preimage))  ;; Bitcoin double-SHA256
        sig-der (ecdsa-sign key-material hash)
        sig-with-hash (byte-array (inc (alength sig-der)))]
    (System/arraycopy sig-der 0 sig-with-hash 0 (alength sig-der))
    (aset sig-with-hash (alength sig-der) (byte 0x01))  ;; SIGHASH_ALL
    ;; Build scriptSig: <varint length> <sig+hashbyte> <varint length> <pubkey>
    ...))
```

### Full Spend Flow

The `live-spend` function ties it all together:

```d2
# Diagram 84
direction: down

START: "live-spend(identity, wallet, recipient, amount)"
FETCH: "get-unspent-with-retry(from-address)"
BUILD: "build-p2pkh-tx(utxos, recipient, amount, fee)"
SIGN: "sign-tx(built-tx, identity)"
HEX: "tx->hex(signed-tx)"
BROAD: "broadcast-tx-with-retry(raw-hex)"
DEDUCT: "wallet/deduct(wallet, amount)"

CHECK: "UTXOs available?" {
  style.fill: "#fafafa"
}
DONE: "Return" {
  style.fill: "#fafafa"
}
ERROR: "Return" {
  style.fill: "#fafafa"
}
OK: "Status :ok?" {
  style.fill: "#fafafa"
}

START -> FETCH
CHECK -> ERROR: "no"
CHECK -> BUILD: "yes"
SIGN -> HEX
BROAD -> OK
OK -> DEDUCT: "yes"
OK -> ERROR: "no"
```

## Step 3: Real OP_RETURN Anchoring

The audit module's `simulate-anchor` was a SHA-256 hash with a fake txid. The replacement `live-anchor` builds a transaction where:

- **Input:** One or more UTXOs from the agent's wallet (to fund the fee)
- **Output 0:** OP_RETURN with `6a` prefix + data push + bundle hash hex
- **Output 1 (optional):** Change output back to agent's address (P2PKH)

```clojure
(defn live-anchor [identity wallet data-hex & {:keys [fee-satoshis]}]
  (let [utxos (get-unspent-with-retry (identity/address identity))
        selection (select-utxos utxos 0 fee)
        inputs (mapv utxo->input (:selected selection))
        anchor-script (str "6a" (format "%02x" (quot (count data-hex) 2)) data-hex)
        outputs [{:satoshis 0 :script-hex anchor-script}]
        outputs (if (pos? (:change selection))
                  (conj outputs {:satoshis (:change selection)
                                 :script-hex (p2pkh-script (pubkey->hash160 pubkey))})
                  outputs)
        signed (sign-all-inputs {:version 1 :inputs inputs :outputs outputs} ...)
        raw-hex (bytes->hex (serialize-tx signed))
        result (broadcast-tx-with-retry raw-hex)]
    ...))
```

The `bundle-and-anchor` function now takes an optional `:wallet` parameter. When present, it broadcasts to testnet. When absent, it falls back to simulation — making all existing tests pass without modification.

## Step 4: BRC-105 Ring Middleware

HTTP 402 Payment Required is the mechanism. The BRCC-105 spec says: server returns 402 with `x-bsv-payment-*` headers, client pays and retries with `x-bsv-payment` header.

```d2
# Diagram 85
shape: sequence_diagram

C: Client
S: Server
L: Ledger

C -> S: GET /api/translate
S -> C: 402 Payment Required {
  style.stroke-dash: 5
}
C -> C: evaluate-payment(wallet, challenge, policy)
C -> L: Broadcast P2PKH payment (500 sats)
L -> C: txid: a1b2c3... {
  style.stroke-dash: 5
}
C -> S: GET /api/translate
S -> S: verify-payment-header(header, 500, pubkey)
S -> C: 200 OK + translation result {
  style.stroke-dash: 5
}
```

The Ring middleware wraps any handler and intercepts configured routes:

```clojure
(defn wrap-brc105-challenge [handler & {:keys [challenge-routes price-satoshis identity-key]}]
  (fn [request]
    (let [payment-header (get-in request [:headers "x-bsv-payment"])]
      (cond
        payment-header
        (if (verify-payment-header payment-header price-satoshis identity-key)
          (handler request)
          {:status 402 :body "invalid-payment"})

        ((set challenge-routes) (:uri request))
        (let [challenge-headers (service-response->challenge price-satoshis identity-key)]
          {:status 402 :headers challenge-headers :body "payment-required"})

        :else (handler request)))))
```

## Step 5: Retry + Resilience

Every external API call (Bitails, WhatsonChain) is wrapped with exponential backoff:

```clojure
(defn with-retry [f & {:keys [max-retries base-delay-ms factor]
                        :or {max-retries 3, base-delay-ms 1000, factor 2}}]
  (loop [attempt 1]
    (let [result (try {:value (f) :error nil}
                      (catch Exception e {:value nil :error e}))]
      (if (:error result)
        (if (< attempt max-retries)
          (do (Thread/sleep (* base-delay-ms (long (Math/pow factor (dec attempt)))))
              (recur (inc attempt)))
          (throw (:error result)))
        (:value result)))))
```

The `refresh-utxos` function provides a recovery path after failed broadcasts:

```clojure
(defn refresh-utxos [identity]
  (let [addr (identity/address identity)
        utxos (get-unspent-with-retry addr)]
    {:address addr :utxos utxos}))
```

## The Test Suite: 100 Tests, 242 Assertions

Every layer has test coverage. The new code added tests for:

| Test Group | Tests | What It Proves |
|------------|-------|----------------|
| Address derivation | 6 | Protocol works, testnet/mainnet bytes, consistency |
| Varint/hex/LE encoding | 5 | Wire format helpers are correct |
| UTXO selection | 2 | Coin selection for sufficient/insufficient funds |
| Transaction structure | 2 | Build P2PKH tx, serialize roundtrip |
| Signing flow | 1 | ScriptSig is non-empty after signing |
| Retry logic | 2 | Returns value, throws after exhaustion |
| BRC-105 middleware | 5 | 402 issued, verified, rejected |
| Faucet/hash160/refresh | 4 | Offline handling, byte lengths |

```d2
# Diagram 86
direction: down

TestResults: "100 tests | 242 assertions | 0 failures" {
  style.fill: "#fafafa"
  AUD: "audit: 15 tests"
  BRC: "brc105: 18 tests"
  COORD: "coordination: 10 tests"
  CORE: "core: 13 tests"
  ID: "identity: 9 tests"
  POL: "policy: 9 tests"
  SDK: "agent-sdk: 10 tests"
  TC: "testnet: 17 tests"
  WAL: "wallet: 9 tests"
}

TestResults.CORE -> TestResults.ID
TestResults.WAL -> TestResults.AUD
TestResults.POL -> TestResults.BRC
TestResults.SDK -> TestResults.COORD
```

```clojure
;; From the REPL:
;; Ran 100 tests containing 242 assertions.
;; 0 failures, 0 errors.
```

## The Architecture Decoupling

The critical design constraint: `coordination.audit` needed to call `coordination.testnet.client/live-anchor`, but `testnet.client` already depended on `audit` for `bytes->hex`. This created a circular dependency.

The solution: pull `hex->bytes` and `bytes->hex` into `testnet.client` as local utilities (they are one-liners), then make `audit` depend on `testnet.client` only optionally through a `:wallet` key in `bundle-and-anchor`.

```d2
# Diagram 87
direction: down

AUD: "audit.clj" {
  style.fill: "#fafafa"
}
RETRY: "testnet.client/with-retry" {
  style.fill: "#fafafa"
}
TC: "testnet.client" {
  style.fill: "#fafafa"
}
TXN: "testnet.client/tx builders" {
  style.fill: "#fafafa"
}

AUD -> TC: "optional: wallet"
TC -> TXN
TC -> RETRY
TXN -> AUD: "no audit dep"
```

No cycles. No shared mutable state. Each module is independently testable.

## The Agile Angle

This implementation followed the Scrum framework from the course: one-week sprints, daily self-inspection, Friday freezes. The product backlog was ordered by dependency (addresses before transactions, transactions before anchoring, anchoring before middleware).

```d2
# Diagram 88
```

The empirical pillars — transparency (open PRs), inspection (test suite), adaptation (breaking the audit cycle) — are not theoretical. They directly shaped the implementation.

## What This Enables

An agent with a BSV keypair and this coordination layer can now:

1. Derive its own testnet address
2. Fetch live UTXO balances from Bitails
3. Build and sign real P2PKH transactions
4. Anchor OP_RETURN audit trails to testnet
5. Request tBSV from the WhatsonChain faucet with confirmation polling
6. Serve and verify BRC-105 HTTP 402 payments via Ring middleware
7. Retry failed network calls with exponential backoff

The next step is the frontier the previous post described: two agents where one hires the other using these primitives. The orchestrator writes a task contract to OP_RETURN, the worker reads it, bonds a micropayment, executes, and settles — all on BSV testnet, all in Clojure, all verifiable by a third party.

The glue is laid. The transactions are real. The test suite says green.
