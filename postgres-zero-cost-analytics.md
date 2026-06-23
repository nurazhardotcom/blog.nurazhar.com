Title: Zero-Backend Analytics: Collapsing Web Tracking to Postgres and GCP for $0
Date: 2026-06-15
Tags: architecture, database, serverless, minimalism, postgres
Description: Why I rejected bloated SaaS trackers, isolated my database boundaries, and built a custom static blog analytics pipeline using only Neon Postgres and GCP Cloud Functions for $0.

---

Yesterday, I wanted to see who was reading this blog. 

Naturally, my first stop was the default GitHub Pages traffic graph. If you've ever looked at it, you know it is aggressively basic: you get a raw count of pageviews and unique visitors over a rolling 14-day window. No geographic data, no device breakdowns, and it completely wipes your history every two weeks. 

I wanted more, but I didn't want to pay, and more importantly, **I wanted to control my own data.**

Here is the journey of how I built an isolated, privacy-first, zero-maintenance analytics engine using only Neon Postgres and GCP Cloud Functions for exactly **$0/month**.

---

## The Lazy Path vs. The Bloat

My search for analytics took me down a familiar rabbit hole of modern web tracking:
1. **The SaaS route (Umami, Plausible, etc.):** Great interfaces, but they either charge a monthly subscription, store my data on third-party servers, or force me to deal with cookie banners and consent frameworks.
2. **The Cloudflare route:** Route the blog's custom domain through Cloudflare and get edge-computed analytics. It's free and script-free, but honestly, I was too lazy to reconfigure DNS records, manage SSL certificates, and sign up for another platform dashboard just to see country metrics.
3. **The Self-Hosted Container route:** Deploying something like GoatCounter (a fantastic Go-based analytics tool) on GCP Cloud Run. But since Cloud Run is stateless, I couldn't use local SQLite storage. I would need to provision a persistent SQL database anyway.

If I have to spin up a database, why not just go native?

---

## Postgres Can Do Everything

We live in an era where PostgreSQL is no longer just a relational database; it is a application foundation. With Neon's generous free tier (giving you 10 branches/databases per project at $0), spinning up a new SQL database takes about three seconds.

The setup is stupidly simple. A single table:

```sql
CREATE TABLE page_views (
    id SERIAL PRIMARY KEY,
    page_path VARCHAR(255) NOT NULL,
    referrer VARCHAR(512),
    country VARCHAR(10) DEFAULT 'Unknown',
    user_agent VARCHAR(512),
    viewed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_page_views_path ON page_views(page_path);
```

To feed this table, we don't need a heavy Node.js or Rails server. We just need a single serverless endpoint that scales to zero when no one is reading.

---

## The Boundary Trap: Resisting Easy Coupling

Since I already have a production payment rail engine (`lagu-lagu`) running on Neon Postgres and GCP Cloud Functions, my first instinct was to just append the tracking code there. One database, one function, zero new setups. 

But this is a dangerous architectural trap.

A personal blog tracker has a completely different lifecycle, risk profile, and security boundary than a financial payment split ledger. Coupling them violates the **Single Responsibility Principle** and introduces a massive security blast radius:

> [!WARNING]
> If a vulnerability is found in the blog tracking API or dashboard code, an attacker could compromise the database credentials and gain read/write access to payment webhook logs and artist payout records.

To prevent this, we chose **strict isolation at zero cost**:
* **Database level:** A completely separate Neon database (`blog_analytics`) with isolated user credentials.
* **Backend level:** A dedicated, lightweight GCP Cloud Function (`blog-analytics-api`) that has no access to the payment schemas.

---

## The $0 Serverless Pipeline

The final architecture is incredibly clean:

```d2
# Diagram 156
direction: down

vars: {
  d2-config: {
    theme-id: 200
  }
}

BlogHTML: {
  label: "blog.nurazhar.com"
}
CF: {
  label: "GCP Cloud Function"
}
DB: {
  label: "(Neon Postgres DB)"
}
StatsDashboard: "stats.html"
track: "track"
stats: "stats"
Views: "Views"

track -> CF
stats -> CF
Views -> DB
```

### 1. The 10-Line Tracker
In `base.html`, we drop a tiny, non-blocking script:

```html
<script>
  (function() {
    if (window.location.hostname === 'localhost') return;
    fetch('https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/blog-analytics-api/track', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        path: window.location.pathname,
        referrer: document.referrer || null
      })
    }).catch(err => console.error(err));
  })();
</script>
```

### 2. Geolocation at the Edge
Instead of loading heavy client-side IP lookup scripts or calling slow external APIs, the GCP Cloud Function reads Google's edge routing headers directly:

```javascript
const country = req.headers['x-appengine-country'] || 'Unknown';
```

Google Cloud automatically geolocates the inbound TCP request at the network edge and injects the ISO country code into the request header. We save it instantly. No cookies, no privacy invasions, and 100% accurate country data.

### 3. The Minimalist Dashboard
To view the stats, we don't load a bloated dashboard dashboard app. We just generated a static `/stats.html` page directly inside the blog. It prompts for a secure passcode, fetches aggregated queries from our function, and renders CSS-only bar charts:

```sql
-- Fetching Top Countries
SELECT country, COUNT(*) as views 
FROM page_views 
GROUP BY country 
ORDER BY views DESC LIMIT 20;
```

---

## The Software Janitor's Victory

By refusing to sign up for more SaaS tools and rejecting database-heavy open-source containers, the blog remains 100% static, fast, and secure.

* **Monthly Cost:** $0 (well within Neon and GCP free tiers).
* **Maintenance:** Zero servers to patch.
* **Control:** The raw SQL data is mine. I can run complex queries, export it, or build custom visualizers whenever I want.

Sometimes, the cleanest solution isn't adding more tools; it's collapsing what you already have into the simplest possible primitives.
