Title: How to Read a Submarine Cable Map — And Why Singapore Has So Many Cables
Date: 2026-06-22
Tags: submarine-cables, networking, singapore, infrastructure, explained
Description: A practical guide to reading submarine cable maps, understanding landing stations, cable routes, and why Singapore is one of the most connected places on earth.

---

Go to [submarinecablemap.com](https://www.submarinecablemap.com/). You see a world map covered in coloured lines crossing oceans, labels like "SEA-ME-WE 6" and "Bifrost", and little dots along coastlines. It looks like someone spilled highlighters on a transit map.

Here's how to actually read it.

---

## The Basics

Every coloured line is a **fibre-optic cable bundle** laid on the ocean floor. Not a single fibre — a bundle. Modern cables carry 8 to 24 fibre pairs, each capable of 20+ Tbps using wavelength-division multiplexing. A single cable can move 200+ Tbps total.

Each cable has:

| Term | Meaning |
|------|---------|
| **Landing point** | Where the cable comes ashore (a small building on the coast) |
| **Landing station** | The facility that connects subsea fibre to terrestrial fibre |
| **RFS date** | Ready for Service — when the cable went live |
| **Length** | Total km of the cable (often 10,000-25,000 km for intercontinental) |
| **Owners** | The consortium or company that built it |

The map shows three types of cables:

- **Solid lines** — Active, carrying traffic today
- **Dashed lines** — Planned or under construction
- **Different colours** — Just for visual separation (not meaningful)

---

## How to Read the Map

### 1. Look at Landing Points, Not Routes

The line on the map is schematic — cables don't follow a straight line. They follow seabed topography, avoid shipping lanes, fishing grounds, and seismic zones. The exact route is surveyed by marine vessels over months.

What matters is **where the cable lands**. Each dot on the coast is a landing station. Click one and you'll see which cables land there.

### 2. Check the RFS Date

Cables from 2002 (like EAC-C2C, 36,500 km) use older technology — maybe 1-2 Tbps per fibre pair. Cables from 2024-2026 (like Bifrost, Apricot, SEA-H2X) use modern coherent optics doing 20+ Tbps per pair. The newer the cable, the more capacity it brings.

### 3. Look for Redundancy

A well-connected location has cables landing at **multiple physical sites** in different directions. If a fishing anchor drags across one cable in the South China Sea, traffic shifts to the other cables. Singapore has cables landing at Changi (east side), Tuas (west side), Tanah Merah and Katong — physically diverse.

### 4. Understand Cable "Hubs"

Some locations are **cable hubs** — places where many cables land and interconnect. Singapore is one. Others: Marseille (Europe-Africa), Egypt (Asia-Europe via Suez), Japan (trans-Pacific), Los Angeles (US-West).

A hub means traffic between any two cables can be patched together at the landing station or nearby data centres.

---

## Why Singapore Has So Many Cables

Open the map and zoom into Singapore. You'll see cables radiating in every direction. Here's why.

### Geography

Singapore sits at the **chokepoint between the Indian Ocean and the South China Sea**. Any cable running:
- Europe ↔ East Asia (via Indian Ocean)
- Middle East ↔ Southeast Asia
- India ↔ China/Japan
- Australia ↔ anywhere north

...has to pass near Singapore. It's the natural branching point.

### History

Singapore has been a cable hub since **1871**, when the first telegraph cable landed from Madras (India). From there it branched to Hong Kong, Saigon, and Darwin. The role never stopped — telegraph became coaxial phone cables became fibre optics. The infrastructure kept building on itself.

### Regulation

Singapore's IMDA has a deliberate policy of attracting cables. Designated landing sites (Changi, Tuas, Tanah Merah), streamlined permitting, and an FBO licensing framework that doesn't block new entrants. This matters because cable consortiums pick landing points based on regulatory predictability.

### Trust

Cable owners care about political stability. A cable costs $300-700 million. You don't land it somewhere that might expropriate it, get tangled in geopolitics, or have unstable power grids. Singapore is one of the few places in Southeast Asia where all parties agree the rule of law is reliable.

---

## The Numbers

As of 2026, Singapore has:

- **~28 active submarine cable systems** landing
- **~13 more planned** (2026-2029)
- **8 cable landing stations** across 4 geographic sites
- **3 designated landing zones**: Changi, Tuas, Tanah Merah

These cables connect Singapore to:

| Region | Example Cables |
|--------|---------------|
| **US (trans-Pacific)** | Bifrost (2025), Echo (2025), SEA-US (via Indonesia) |
| **Japan / Korea** | SJC2 (2025), APG (2016), EAC-C2C (2002) |
| **China / Hong Kong** | APG, ASE (2012), SJC2, TGN-IA (2009) |
| **Europe / Middle East** | SEA-ME-WE 6 (2026), SEA-ME-WE 5 (2016), PEACE (2022) |
| **India** | IAX (2024), MIST (2024), Tata TGN (2004) |
| **Australia** | INDIGO-West (2019), ACC-1 (2028 planned) |
| **Indonesia** | BSCS (2009), IGG (2018), Nongsa-Changi (2026) |

Tuas alone (western Singapore) has **16 cable landings** — Apricot, ADC, Bifrost, IAX, INDIGO-West, IGG, MIST, PEACE, SEA-H2X, SEA-ME-WE 4/5/6, SJC, and more. Changi North has **11 landings** including Echo, AAG, and EAC-C2C.

---

## What This Means for Your Internet

### 1. Low Latency

Your packet to Cloudflare (4ms), Google (5ms), or AWS (3ms) never leaves Singapore because those providers have edge nodes inside the country. For traffic that does leave:

| Destination | Approx Latency | Cable Used |
|-------------|---------------|-----------|
| Tokyo | 50-70ms | APG or SJC2 |
| Los Angeles | 120-140ms | Bifrost or Echo |
| London | 150-180ms | SEA-ME-WE 6 |
| Sydney | 60-80ms | INDIGO-West |
| Mumbai | 60-80ms | IAX or MIST |

### 2. Resilience

If you visit submarinecablemap.com and see 5 cables going from Singapore to Japan — if one breaks (ship anchor, earthquake, shark bite — yes, sharks bite cables), the other 4 carry the traffic. BGP reroutes in seconds. You might see a single jittery packet, or nothing at all.

### 3. Competition

Multiple cables means no single carrier owns your route. Singtel, StarHub, Telstra, Google, Meta, China Mobile, Reliance Jio — they all have their own cable capacity or shares in consortia. This drives wholesale bandwidth prices down. Singapore has some of the cheapest international bandwidth in Asia because of this competition.

### 4. The Hyperscaler Effect

Google and Meta now build their own cables (Echo, Bifrost, Apricot). They don't wait for telecom consortia. They land them in Singapore because the regulatory environment lets them. This means Google Cloud and Meta's services get dedicated fibre pairs — not shared with everyone else's traffic.

---

## How Your Packet Uses These Cables

From my machine in Ulu Bedok:

1. **GPON ONT** → Singtel BRAS (Chai Chee or Jurong)
2. **Singtel's network** → Equinix SG data centre (Tai Seng area)
3. **At Equinix**: Singtel hands traffic to the cable consortium
4. **The cable**: SEA-ME-WE 6 west to Europe, or Bifrost east to US, or APG north to Japan
5. **At the far end**: Landing station → terrestrial fibre → data centre → server

The cable map shows steps 4 and 5. Steps 1-3 happen inside Singapore in single-digit milliseconds.

---

## The Map's Limitations

The map doesn't tell you:

- **Capacity** — a thin line might carry 200 Tbps, a thick line 10 Tbps. The map doesn't show bandwidth.
- **Utilisation** — how full each cable is. Some cables from 2002 are nearly maxed out; new cables from 2024 have headroom.
- **Ownership** — who actually controls the capacity. Singtel is on most consortia; Google owns dedicated pairs on Echo.
- **Terrestrial backhaul** — what happens after the cable lands. If the terrestrial fibre from Tuas to the data centre is a single route, that's a weak point even with 10 subsea cables.

---

## Practical Takeaway

When you look at submarinecablemap.com:

1. **Zoom to Singapore** — see the density. That's not normal for most countries.
2. **Click landing points** — compare Tuas (16 cables, mostly west-facing) vs Changi (11 cables, mostly east-facing).
3. **Check RFS dates** — cables from 2024-2026 use modern tech with way more capacity.
4. **Trace your traffic's likely path** — to a US server, probably Bifrost or Echo. To Europe, SEA-ME-WE 6 or PEACE. To Japan, APG or SJC2.
5. **Appreciate the redundancy** — if any single cable gets cut, there are 3-5 alternatives between Singapore and any major destination.

That's why your Singtel connection feels fast and reliable — not magic, just 28 cables and 150 years of infrastructure compounding.

---

*Sources: TeleGeography Submarine Cable Map (2026), CSIS Strategic Analysis (2025), IMDA Submarine Cable Guidelines (2026), Submarine Networks Singapore. Some data points from personal packet traces on Singtel Fibre (AS9506).*
