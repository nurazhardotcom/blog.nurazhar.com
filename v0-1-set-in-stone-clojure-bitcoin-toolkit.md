Title: Bitcoin v0.1 Set in Stone: Building a functional blockchain toolkit in Clojure
Date: 2026-06-09
Tags: clojure, bitcoin, blockchain, web-development, functional-programming, repl
Description: A deep dive into bsv-clj, a Clojure toolkit built to explore Bitcoin's original, immutable protocol design through a JSON-RPC client, wallet toolkit, and custom block explorer.

---

Over the last few days, I've been deep in the blockchain archives. I wanted to build a portfolio project that would let me study the core engine of peer-to-peer electronic cash without wrestling with an ever-mutating spec sheet. 

I ended up building **`bsv-clj`**—a complete Clojure toolkit featuring an idiomatic JSON-RPC client, a read-only wallet tool, and a Ring/Hiccup-powered local block explorer. It connects directly to a **Bitcoin SV (BSV)** node. 

Here is the story of how I designed it, and why Clojure and Bitcoin's original v0.1 protocol are a perfect match.

---

## 1. The Design Philosophy: Locking the Protocol

On June 17, 2010, Satoshi Nakamoto wrote a post on the Bitcoin forum that would define a core fork in blockchain history:

> *"The nature of Bitcoin is such that once version 0.1 was released, the core design was set in stone for the rest of its lifetime."*

In modern blockchain development, this philosophy is rarely followed. BTC has introduced protocol changes like SegWit and Taproot; other chains rewrite their consensus rules every year. But the Bitcoin SV node client preserves the original v0.1 protocol spec: no arbitrary block size limits, no complex alterations of basic opcodes, and the original UTXO transactional mechanics.

For an engineer wanting to study the system, this stability is a superpower:
*   The **RPC interface** maps directly to Satoshi's original client design.
*   The **transaction schema** and **UTXO model** are clean, minimal, and stable.
*   You are querying the foundational data structures that launched the entire industry in 2009.

With this stable backend in place, my goal was to wrap it in an idiomatic Clojure layer to create the ultimate interactive learning environment.

---

## 2. Why Clojure + Bitcoin?

As I built the toolkit, I realized that Bitcoin's core mechanics are fundamentally functional. The parallels between Bitcoin's protocol and Clojure's standard library are striking:

| Bitcoin Protocol Concept | Clojure Equivalent |
|---|---|
| Transactions are **immutable** once written | Clojure's **immutable data structures** |
| UTXOs are consumed and created, never modified | Pure functions returning new state with **no side effects** |
| Script is a **stack-based** language | Clojure's **list and sequence abstractions** |
| Blocks form an **append-only** chain | Persistent data structures with **structural sharing** |
| Data-driven API (JSON-RPC) | Clojure is **data-oriented** by design |

When you query a Bitcoin transaction, you are given a map of inputs and outputs. An input consumes a past output; new outputs declare future UTXOs. There is no `balance` column in a database. Balance is a derived projection of the unspent UTXO set. In Clojure, this feels like home: you simply filter and reduce a collection of maps.

---

## 3. Designing `bsv-clj`

The codebase is split into three main modules:

### A. The Core RPC Client
Instead of writing a dynamic helper for each of Bitcoin's hundred-plus RPC methods, I built a lean HTTP client in `bsv.rpc.client` that acts as a generic, data-driven wrapper around JSON-RPC 1.0:

```clojure
(defn rpc-call [cfg method params]
  (let [body (json/generate-string {:jsonrpc "1.0"
                                    :id (str (java.util.UUID/randomUUID))
                                    :method method
                                    :params params})
        response (client/post (rpc-url cfg)
                              {:basic-auth [(:user cfg) (:password cfg)]
                               :body body
                               :content-type :json
                               :as :json})]
    (get-in response [:body :result])))
```

This single function handles the authentication, HTTP POST request, JSON encoding/decoding via Cheshire, and error wrapping. On top of this core function, I built high-level modules for `blockchain`, `transaction`, `network`, and `mining` queries.

### B. The Wallet Toolkit
The wallet module is read-only for safety. It queries the node for the active UTXO set, tracks balances, and resolves address balances. Rather than relying on heavyweight database indexes, it extracts information directly from the node's mempool and block databases, showing how Clojure's sequence processing makes it trivial to filter and group blockchain data:

```clojure
(defn group-by-address [utxos]
  (reduce (fn [acc utxo]
            (let [addr (:address utxo)
                  amount (:amount utxo)]
              (update-in acc [addr] (fnil + 0.0) amount)))
          {}
          utxos))
```

### C. The Hiccup Block Explorer
To visualize the blocks and transactions, I built a local web-based explorer using **Ring**, **Compojure**, and **Hiccup**. 

Instead of dealing with a complex JavaScript frontend framework (like React or Vue) or pulling in Tailwind, I used vanilla CSS to style a beautiful, dark-themed dashboard. Hiccup compiles standard Clojure vectors directly into HTML:

```clojure
(defn block-page [block]
  (layout
    (str "Block #" (:height block))
    [:div.container
     [:h1 (str "Block #" (:height block))]
     [:div.card
      [:table
       [:tr [:td "Hash"] [:td (:hash block)]]
       [:tr [:td "Time"] [:td (format-time (:time block))]]
       [:tr [:td "Transactions"] [:td (count (:tx block))]]]]]))
```

The application launches instantly, renders server-side, and runs at 60 FPS without downloading a single megabyte of frontend JS bundles.

---

## 4. The REPL Experience

The magic of Clojure is the REPL. When you run `clj -A:dev`, the REPL starts and loads the `user` namespace, which exposes immediate development helpers. 

Instead of relying on log statements or debugger breakpoints, you can interact with the blockchain in real-time:

```clojure
;; Connect to local node
(connect! "localhost" 8332 "rpcuser" "rpcpass")

;; Check node synchronization
(status)
;; => {:height 850123, :difficulty 1293847291.82, :connections 8}

;; Grab the latest 3 blocks
(latest 3)
;; => ({:height 850123, :hash "00000..."} ...)

;; Start the local Ring server
(start!)
;; => "Server started at http://localhost:3000"
```

If you change a route in the explorer or modify the styling, you don't need to rebuild the project or restart the server. Clojure's dynamic classloading allows you to re-evaluate the namespace inside your running REPL, and the changes appear instantly on your next page refresh.

---

## Conclusion

Building `bsv-clj` reinforced my belief that **the data model is the program**. 

By stripping away the bloat of mutable state, heavy frameworks, and shifting protocols, I was able to build a portfolio-quality blockchain toolkit in under 2,500 lines of Clojure. It serves as both a stable toolkit for blockchain protocol research and a demonstration of Clojure's data-oriented design.

You can inspect the codebase and try it yourself at: **[github.com/nurazhardotcom/bsv-clj](https://github.com/nurazhardotcom/bsv-clj)**.
