Title: From Python to Babashka: Rewriting a Crypto ETL Pipeline in Clojure
Date: 2026-06-18
Tags: clojure, babashka, python, etl, sqlite, bitcoin, bsv, data-engineering, automation, rewrite
Description: A hands-on walkthrough of porting a Python + SQLite crypto price tracker to Babashka (Clojure scripting runtime) — covering the ETL design, the JDBC trap I hit, and why shelling out to sqlite3 turned out to be the cleanest solution.
---

## The Original: Python + SQLite

I had a small Python project — `sol-de-tracker` — that fetches the current Solana price from CoinGecko and stores it in SQLite. Two scripts, ~60 lines total:

```python
# get_sol_price.py
import requests

API_URL = "https://api.coingecko.com/api/v3/simple/price?ids=solana&vs_currencies=sgd"

response = requests.get(API_URL)

if response.status_code == 200:
    data = response.json()
    sol_price = data.get('solana', {}).get('sgd')
    print(f"Current SOL Price (SGD): {sol_price}")
else:
    print(f"Error fetching data: {response.status_code}")
```

The extended version (`get_sol_price_with_db.py`) adds `sqlite3` from the standard library, creates a table, and inserts each price point with a timestamp. Classic ETL: Extract from API → Transform (validate) → Load into DB.

It worked. But I wanted to see what this looks like in **Babashka** — Clojure's fast scripting runtime. And then I changed the tracked asset from SOL to BSV (Bitcoin SV), which gave me a chance to validate the full rewrite end-to-end.

---

## Why Babashka?

Babashka (`bb`) is a GraalVM-compiled Clojure runtime that starts in ~5ms. No JVM warm-up, no `deps.edn` download ceremony — just a single static binary and go. For scripting tasks like ETL, it's compelling:

| Aspect | Python 3.10+ | Babashka |
|---|---|---|
| Startup | ~30ms | ~5ms |
| HTTP | `requests` (pip) | `babashka.http-client` (built-in) |
| JSON | `json` (stdlib) | `cheshire` (built-in) |
| Database | `sqlite3` (stdlib) | `clojure.java.shell` + `sqlite3` CLI |
| Deployment | Python interpreter + venv | Single static binary |
| Package manager | pip + requirements.txt | `bb.edn` (deps.edn format) |

The built-in libraries cover most scripting needs: HTTP client, JSON parsing, filesystem operations, process management, and shell interop. No `pip install`, no virtualenv, no dependency hell.

---

## The Rewrite

### Script 1: Extract Only

The standalone price fetcher is a direct port. Babashka's HTTP client and JSON parser are built-in — no dependencies needed:

```clojure
#!/usr/bin/env bb

(ns get-bsv-price
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]))

(def api-url
  "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin-cash-sv&vs_currencies=sgd")

(defn fetch-bsv-price []
  (let [response (http/get api-url {:throw false})]
    (if (= 200 (:status response))
      (let [data      (json/parse-string (:body response) true)
            bsv-price (get-in data [:bitcoin-cash-sv :sgd])]
        bsv-price)
      (do
        (println (str "Error fetching data: " (:status response)))
        nil))))

(defn -main []
  (if-let [price (fetch-bsv-price)]
    (println (format "Current BSV Price (SGD): %s" price))
    (println "Failed to fetch BSV price.")))

(-main)
```

Key patterns:

- **`:throw false`** — prevents exceptions on non-200 status codes, mirrors the Python `if status_code == 200` pattern
- **`if-let`** — Clojure's nil-safe binding + branch, equivalent to Python's `if value is not None`
- **`get-in`** — navigates nested maps with keywords, equivalent to Python's chained `.get()`

### Script 2: Extract + Load (The Interesting Part)

This is where things got tricky. The Python version uses `sqlite3` from the standard library — a batteries-included JDBC wrapper. In Babashka, the situation is different.

---

## The JDBC Trap

My first instinct was to use `next.jdbc`, the most popular Clojure JDBC library. I added it as a Maven dependency in `bb.edn`:

```clojure
{:deps {org.xerial/sqlite-jdbc {:mvn/version "3.46.1.3"}
        com.github.seancorfield/next.jdbc {:mvn/version "1.3.909"}}}
```

This downloaded successfully but crashed at analysis time:

```
Unable to resolve symbol: java.beans.Introspector/getBeanInfo
```

The Maven version of `next.jdbc` depends on `java.beans.Introspector`, which isn't available in Babashka's GraalVM sandbox. It's a core Java class that's stripped from the native image.

Next attempt — raw `java.sql` interop:

```clojure
(import '[java.sql DriverManager Connection Statement ResultSet])
```

Same wall. `java.sql.DriverManager` isn't available either. Babashka's GraalVM build excludes most of the `java.sql` module.

**The lesson:** Babashka's "built-in" JDBC support doesn't mean you can use arbitrary JDBC drivers or Java interop with `java.sql`. The built-in namespace may not be present in all versions, and the underlying Java classes are restricted.

---

## The Clean Solution: Shell Out

Babashka has `clojure.java.shell` built-in, and `sqlite3` CLI is available on virtually every Linux system. The idiomatic Babashka approach for database operations is to shell out:

```clojure
#!/usr/bin/env bb

(ns get-bsv-price-with-db
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.java.shell :refer [sh]]))

(def api-url
  "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin-cash-sv&vs_currencies=sgd")

(def db-file "bsv_data.db")

(defn fetch-bsv-price []
  (let [response (http/get api-url {:throw false})]
    (if (= 200 (:status response))
      (let [data      (json/parse-string (:body response) true)
            bsv-price (get-in data [:bitcoin-cash-sv :sgd])]
        {:price bsv-price})
      (do
        (println (str "Error fetching data: " (:status response)))
        nil))))

;; --- Database helpers ---

(defn sqlite! [& args]
  (let [result (apply sh "sqlite3" db-file args)]
    (when-not (zero? (:exit result))
      (throw (ex-info (str "SQLite error: " (:err result))
                      {:exit (:exit result) :err (:err result)})))
    (:out result)))

(defn init-schema! []
  (sqlite! (str "CREATE TABLE IF NOT EXISTS prices ("
                "  timestamp TEXT PRIMARY KEY,"
                "  currency  TEXT,"
                "  price     REAL"
                ");")))

(defn save-price! [price currency timestamp]
  (when-not (number? price)
    (throw (ex-info "Invalid price from API" {:price price})))
  (sqlite! (str "INSERT INTO prices (timestamp, currency, price) "
                "VALUES ('" timestamp "', '" currency "', " price ");")))

(defn current-timestamp []
  (let [now (java.time.LocalDateTime/now)
        fmt (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format now fmt)))

;; --- Main ---

(defn -main []
  (if-let [{:keys [price]} (fetch-bsv-price)]
    (let [timestamp (current-timestamp)
          currency  "SGD"]
      (println (format "Current BSV Price (SGD): %s" price))
      (try
        (init-schema!)
        (save-price! price currency timestamp)
        (println "Data point saved successfully to bsv_data.db.")
        (catch Exception e
          (println (str "Database Error: " (.getMessage e))))))
    (println "Failed to fetch BSV price.")))

(-main)
```

The `sqlite!` helper wraps `clojure.java.shell/sh` with error checking. It's 8 lines of code, zero dependencies, and works everywhere `sqlite3` is installed.

The `save-price!` function includes a numeric validation guard — if CoinGecko ever returns `null` or a non-numeric value for the price, it throws a descriptive error instead of generating malformed SQL.

---

## Project Structure

The final project has zero external dependencies:

```
bsv-de-tracker/
├── bb.edn                    # {} — empty, everything is built-in
├── get_bsv_price.bb          # Standalone price fetcher
├── get_bsv_price_with_db.bb  # Price fetcher + SQLite persistence
└── README.md
```

Run it:

```bash
bb get_bsv_price.bb           # Current BSV Price (SGD): 15.42
bb get_bsv_price_with_db.bb   # ... saved to bsv_data.db
```

Verify the data:

```bash
sqlite3 bsv_data.db "SELECT * FROM prices;"
# 2026-06-18 20:15:32|SGD|15.42
```

---

## Automation

Same as the Python version — a cron job:

```bash
# Run at minute 0 of every hour
0 * * * * /path/to/bb /path/to/bsv-de-tracker/get_bsv_price_with_db.bb >> error.log 2>&1
```

Or a `systemd` timer for more control. The Babashka binary starts fast enough that cron overhead is negligible.

---

## Takeaways

1. **Babashka is not a JVM.** The GraalVM sandbox strips out `java.sql`, `java.beans`, and other core Java modules. Don't assume Java interop "just works" — test it.

2. **Shelling out is idiomatic.** For SQLite, CLI tools, and other system utilities, `clojure.java.shell` is the Babashka way. It's clean, dependency-free, and the `sqlite3` CLI is more battle-tested than most JDBC drivers.

3. **"Built-in" means "available in some versions."** The `next.jdbc` namespace may or may not be present depending on your Babashka version. Always verify with `bb -e '(require (quote [next.jdbc :as jdbc]))'` before depending on it.

4. **Zero-dependency scripts are powerful.** The entire project — HTTP client, JSON parser, SQLite persistence — runs with `bb` and `sqlite3`. No package manager, no lock files, no containers.

5. **The rename from SOL to BSV was mechanical.** CoinGecko uses `bitcoin-cash-sv` as the BSV coin ID. Changing the API URL, variable names, and database file took 2 minutes. The code structure didn't change at all.

---

## Links

- **Source:** [sol-de-tracker](https://gitlab.com/nurazhar/sol-de-tracker) (original Python)
- **Babashka:** [babashka.org](https://babashka.org/)
- **CoinGecko API:** [coingecko.com/api](https://www.coingecko.com/en/api)
- **quickblog:** [github.com/borkdude/quickblog](https://github.com/borkdude/quickblog) (this blog's engine)
