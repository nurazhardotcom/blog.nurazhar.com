Title: Scaling Micro-Transactions: Many-to-One Payout Batching inside Postgres
Date: 2026-06-15
Tags: postgres, database, architecture, optimization
Description: How to implement threshold-based payout batching using many-to-one database relations and PL/pgSQL triggers to minimize on-chain notary fees and transaction costs.

---

In a "No-Backend" architecture where the database is the primary application engine, writing clean and optimized business logic directly inside the database is key. 

Recently, while building **[lagu-lagu](https://gitlab.com/nurazhar/lagu-lagu)**—a stateless payout registry for independent musicians—we ran into a common micro-transaction scaling bottleneck: **notary fee bloat**. 

If a fan tips an artist $1.00, triggering an outbound bank payout (via Tazapay) and committing a cryptographic proof to a public ledger (BSV Notary API) for *every single transaction* eats up all the artist's micro-liquidity in network transaction fees. 

The solution? **Threshold Payout Batching** implemented natively in Postgres.

---

## 1. Shifting from 1-to-1 to Many-to-One Relations

Initially, our `payouts` table had a `transaction_id` column with a `UNIQUE` constraint. This enforced a strict 1-to-1 relationship between fan payments and artist payouts:

```d2
# Diagram 154
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

OldArchitecture: {
  label: "Old Architecture: 1-to-1"
  P1: "Artist Payout"
  Tx: "Fan Transaction"
}
```

To support batching, we inverted this relation. A single `payout` can clear multiple incoming `transactions`. The database structure became **many-to-one**:

```d2
# Diagram 155
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

NewArchitecture: {
  label: "New Architecture: Many-to-One"
  Artist: "Artist Wallet - $9.60"
  P: "Grouped Payout - $12.00"
  Platform: "Platform Wallet - $2.40"
  T1: "Fan Transaction 1 - $4.00"
  T2: "Fan Transaction 2 - $4.00"
  T3: "Fan Transaction 3 - $4.00"
  
  P -> Artist: "80% Artist Split"
  P -> Platform: "20% Commission"
}
```

### Architectural Comparison

| Feature | Standard 1-to-1 Payouts | Many-to-One Threshold Batched Payouts |
| :--- | :--- | :--- |
| **Relationship Schema** | `payouts` holds `transaction_id (Unique)` | `transactions` holds `payout_id (Nullable)` |
| **Notary Cost (BSV)** | $O(N)$ - Fee paid on *every single transaction* | $O(1)$ - Fee paid once per threshold batch |
| **Outbound Disbursals** | Instant payout on every single stream/tip | Grouped payout once threshold (e.g., $10) is met |
| **Integrity Enforcement** | Hard database unique constraint | Cascade deletion & Set Null on payout removal |

In SQL, this is implemented by:
1. Removing the `transaction_id` column from the `payouts` table.
2. Adding a nullable `payout_id UUID REFERENCES payouts(id) ON DELETE SET NULL` to the `transactions` table.

---

## 2. Natively Batching with PL/pgSQL Triggers

Instead of spinning up a background cron job (which introduces latency and state drift), we use an **`AFTER UPDATE` row trigger** in Postgres to calculate and batch balances on the fly. 

When a transaction status is updated to `'success'`, the trigger calculates the current pending balance of all successful transactions for the artist that have not yet been paid out:

```sql
CREATE OR REPLACE FUNCTION process_payment_split()
RETURNS TRIGGER AS $$
DECLARE
    v_artist_id UUID;
    v_pending_amount NUMERIC(12, 4);
    v_amount_sent NUMERIC(12, 4);
    v_commission_retained NUMERIC(12, 4);
    v_payout_id UUID;
    v_threshold NUMERIC(12, 4) := 10.0000; -- SGD 10.00 threshold
BEGIN
    -- Trigger split when transaction status updates to 'success'
    IF NEW.status = 'success' AND (TG_OP = 'INSERT' OR OLD.status IS NULL OR OLD.status != 'success') THEN
        -- Find the artist associated with the track
        SELECT artist_id INTO v_artist_id FROM tracks WHERE id = NEW.track_id;

        -- Sum all successful, unpaid transactions for this artist
        SELECT COALESCE(SUM(t.amount), 0) INTO v_pending_amount
        FROM transactions t
        JOIN tracks tr ON t.track_id = tr.id
        WHERE tr.artist_id = v_artist_id
          AND t.status = 'success'
          AND t.payout_id IS NULL;

        -- If the accumulated balance meets or exceeds the threshold, batch the payout
        IF v_pending_amount >= v_threshold THEN
            v_amount_sent := v_pending_amount * 0.80; -- 80% to artist
            v_commission_retained := v_pending_amount * 0.20; -- 20% commission

            -- Create a single grouped payout record
            INSERT INTO payouts (artist_id, amount_sent, commission_retained, currency, status)
            VALUES (v_artist_id, v_amount_sent, v_commission_retained, NEW.currency, 'pending')
            RETURNING id INTO v_payout_id;

            -- Update all pending transactions to point to this settling payout_id
            UPDATE transactions
            SET payout_id = v_payout_id
            WHERE id IN (
                SELECT t.id
                FROM transactions t
                JOIN tracks tr ON t.track_id = tr.id
                WHERE tr.artist_id = v_artist_id
                  AND t.status = 'success'
                  AND t.payout_id IS NULL
            );
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

---

## 3. The Stateless Proxy Loop

Because the database handles the complex state transitions and batching internally, the serverless API router (GCP Cloud Function / Cloudflare Worker) remains completely stateless:

1. The payment gateway webhook sends a `success` callback to the GCP Cloud Function.
2. The function inserts the transaction into Neon Postgres.
3. If the trigger finds the artist's total pending balance is below the threshold, it commits the write and immediately returns a success message to the gateway (payout pending).
4. If the threshold is reached, the database compiles the payout record and returns the new `payout_id`. The function detects this, calls the outbound Tazapay settlement API, submits a single Merkle notary proof on the blockchain, and commits.

---

## Conclusion: Thermodynamic Software Engineering

By shifting the computational logic directly to Postgres constraints and trigger functions:
* We achieve **zero CPU overhead** on the API proxy gateway.
* We **eliminate network transaction costs** by $N$ times, ensuring micro-payments remain profitable for creators.
* We preserve the **single source of truth** directly inside database relations.
