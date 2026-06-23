Title: The 15-Minute VAPT: Zero-Backend Security on Neon and GCP
Date: 2026-06-14
Tags: security, serverless, gcp, postgres, htmx, vapt, finance
Description: Traditional enterprise security audits take months and cost thousands. Here is how we audited, patched, and redeployed a secure, PDPA-compliant serverless payment split engine in under 15 minutes.

---

In enterprise environments (like Singapore's defense and corporate sectors), **Vulnerability Assessment and Penetration Testing (VAPT)** is typically a multi-week bureaucratic slog. You pay a consultant $20,000+, they run standard scanner suites, generate a bloated 50-page PDF report of generic warnings, and hand it back to your developers to spend three months debating and patching.

It is a high-overhead, low-velocity cycle.

But when you build using a **"No-Backend" architecture**—eliminating custom middleware servers in favor of static edge clients, serverless database triggers, and a stateless proxy—the attack surface shrinks so dramatically that you can perform a thorough VAPT, write custom sanitizers, harden schema constraints, and redeploy to production in **less than 15 minutes**.

Here is how we did exactly that for `lagu-lagu` (a real-time Singapore PayNow payout registry for independent artists).

---

## 🏗️ The Minimalist Surface Area

Our application uses a zero-server runtime model:
1.  **Frontend:** Static HTML and **HTMX** served from GitHub Pages (100% static CDN).
2.  **Database:** Serverless **Neon Postgres**, with Row-Level Security (RLS) enabled and a PL/pgSQL database trigger calculating the 80/20 split on payment insertion.
3.  **API Gateway:** A stateless **GCP Cloud Function (Node.js)** that secures connection strings, handles incoming webhooks, and returns pre-rendered HTML snippets.

By deleting standard backend servers (like Spring Boot or Express), we deleted 99% of our dependency overhead. We don't have server routes to exploit, memory leaks to exploit, or unpatched framework packages. 

But minimalist doesn't mean bulletproof. We audited the system and found three real vulnerabilities.

---

## 🔍 The VAPT Checklist & Exploits

### 1. Stored XSS via HTML Template Injection (High Severity)
Our GCP function was querying the database and generating HTML string templates dynamically to serve back to the HTMX frontend:
```javascript
const html = rows.map(artist => `<h3>${artist.name}</h3>...`).join("");
```
*   **The Exploit:** If an attacker registered an artist name containing a malicious script tag (e.g., `<script>steal(document.cookie)</script>`), Postgres would store it, the GCP function would fetch it, and the browser would execute it.
*   **The Fix:** We wrote a lightweight, native HTML escaping function inside Node.js and wrapped all dynamic SQL variables before concatenation:
    ```javascript
    function escapeHtml(unsafe) {
      if (!unsafe) return '';
      return unsafe.toString()
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
    }
    ```

### 2. Lack of DB Constraints on Payment Splits (Low Severity)
The split trigger calculated the 80/20 division using standard multiplication (`NEW.amount * 0.80`). But the schema did not explicitly validate that payment amounts were positive.
*   **The Exploit:** If a negative transaction amount (e.g. `SGD -10.00`) bypassed frontend validation or was injected via a rogue webhook, the database would generate a negative artist payout (`SGD -8.00`), causing financial ledger logic corruption.
*   **The Fix:** We executed raw SQL queries to add constraints directly to the live Neon instance:
    ```sql
    ALTER TABLE transactions ADD CONSTRAINT check_positive_amount CHECK (amount > 0);
    ALTER TABLE payouts ADD CONSTRAINT check_positive_payout CHECK (amount_sent > 0);
    ```

### 3. Error Verbosity Leak (Low Severity)
In our try-catch block, we returned raw stack errors to the client:
```javascript
catch (error) {
  return res.status(500).json({ error: error.message });
}
```
*   **The Exploit:** If a query crashed, Postgres table names, column structures, and connection parameters would leak directly in the browser console.
*   **The Fix:** We redirected all detailed traces to GCP Cloud Logging and genericized the HTTP client response to a sanitized string:
    ```javascript
    catch (error) {
      console.error("Internal API error:", error);
      return res.status(500).json({ error: "Internal Server Error" });
    }
    ```

---

## 🔒 Proactive Prevention: GitHub Push Protection

To ensure we never leak a connection string or API token in the future, we used the GitHub CLI to globally configure security settings for our repos:
```bash
echo '{"security_and_analysis":{"secret_scanning":{"status":"enabled"},"secret_scanning_push_protection":{"status":"enabled"}}}' \
  | gh api --method PATCH repos/nurazhardotcom/lagu-lagu --input -
```
With **Push Protection** enabled, GitHub will intercept and block any git push command at the CLI layer if it detects an exposed password or token before it is committed to the cloud.

---

## 💡 The Security Lesson

Modern security compliance (like Singapore's Cyber Trust Mark or corporate checklists) tends to focus on administrative processes, PDF paperwork, and constant scanning. 

But **complexity is the ultimate enemy of security**. 

The safest line of code is the one you never wrote. By keeping your backend database-native, relying on Postgres constraints for business logic, and routing calls via stateless Cloud Functions, you keep your system so simple that VAPT goes from a multi-week corporate bottleneck to a 15-minute engineering sprint.
