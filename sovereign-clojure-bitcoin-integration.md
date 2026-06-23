Title: Clojure & Bitcoin: Building a Sovereign Node & Integration Sandbox
Date: 2026-06-14
Tags: clojure, bitcoin, sandbox, security, architecture
Description: An engineering overview of integrating Clojure with a native Bitcoin node RPC, sanitizing UTXO wallet outputs from SQLite, and using Malli schema validation for data safety.

---

Lisp (specifically Clojure) and Bitcoin are a natural match. Both prioritize immutable data structures, declarative transformations, and minimal state side-effects. 

In this post, we'll walk through the architectural blueprint of **[bsv-clj](https://github.com/nurazhardotcom/bsv-clj)**, a sandbox project showing how to query Bitcoin node RPCs, map spendable UTXOs from local SQLite databases, validate transaction payloads, and redact sensitive data at the edge.

---

## 1. Node RPC Client Plumbing

The first layer is talking to the Bitcoin node itself over JSON-RPC. Using `http-kit` and `cheshire` (JSON parser), we can call remote node methods natively:

```clojure
(def rpc-config
  {:url "http://127.0.0.1:8332"
   :user "rpcuser"
   :password "rpcpassword"})

(defn rpc-call
  "Executes a JSON-RPC method call on the Bitcoin node."
  ([method] (rpc-call method []))
  ([method params]
   (let [payload (json/generate-string
                  {:jsonrpc "2.0"
                   :id "clj-rpc"
                   :method method
                   :params params})
         options {:headers {"Content-Type" "application/json"}
                  :basic-auth [(:user rpc-config) (:password rpc-config)]
                  :body payload}
         response @(http/post (:url rpc-config) options)]
     (if (= 200 (:status response))
       (json/parse-string (:body response) true)
       {:error (str "HTTP Error: " (:status response))}))))
```

With this, running `(rpc-call "getblockchaininfo")` returns a native Clojure map representing the blockchain status.

---

## 2. Local-First Sandbox: UTXO Tracking via SQLite

Instead of querying third-party APIs (which exposes your financial privacy), the sovereign way is to read directly from your local wallet database. 

Using `next.jdbc` and `honey.sql`, we query spendable outputs (UTXOs) from a local wallet sandbox:

```clojure
(defn query-wallet-utxos
  "Queries local SQLite database to list spendable outputs."
  [db-path]
  (let [ds (jdbc/get-datasource {:dbtype "sqlite" :dbname db-path})
        query (sql/format {:select [:txid :outputIndex :amount :state]
                           :from [:outputs]
                           :where [:= :state "SPENDABLE"]})]
    (jdbc/execute! ds query)))
```

---

## 3. Transaction Validation with Malli

To prevent malformed transaction request structures from reaching signing nodes, we define a declarative schema using **[Malli](https://github.com/metosin/malli)**:

```clojure
(def TransactionRequestSchema
  [:map
   [:appId :string]
   [:inputs [:vector [:map
                      [:txid [:re #"^[a-fA-F0-9]{64}$"]]
                      [:outputIndex :int]]]]
   [:outputs [:vector [:map
                        [:toAddress [:re #"^[13m-n2-9][a-km-zA-HJ-NP-Z1-9]{26,34}$"]]
                        [:satoshis [:pos-int]]]]]
   [:metadata [:map
               [:timestamp :int]
               [:data :string]]]])
```

This enforces strict hexagonal boundaries: matching valid transaction hex patterns, valid Bitcoin addresses, positive satoshi spends, and clean metadata.

---

## 4. Privacy Sentry: Data Loss Prevention (DLP)

When attaching raw text metadata directly into transaction payloads or OP_RETURN scripts, leakages can happen. We inject a regex redaction layer to scrub sensitive Singapore NRIC profiles before they hit the blockchain:

```clojure
(def sg-nric-regex #"(?i)\b[SGTFAM]\d{7}[A-Z]\b")

(defn redact-payload-metadata
  "Scans metadata text and redacts sensitive Singapore NRIC profiles."
  [text]
  (clojure.string/replace text sg-nric-regex "[REDACTED_NRIC]"))
```

---

## Why this Architecture Fits the Bitcoin Ethos

By putting the RPC clients, local SQLite DBs, and payload validation under a Clojure workflow, we achieve:
* **Determinism**: Zero side-effects inside key validations.
* **Hermetic Runs**: No dependency on cloud providers to run rules.
* **Privacy**: Data stays local and is scrubbed before broadcast.

You can check out the sandbox project source code on GitHub: **[github.com/nurazhardotcom/bsv-clj](https://github.com/nurazhardotcom/bsv-clj)**.
