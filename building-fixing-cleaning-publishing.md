Title: Building, Fixing, Cleaning, and Publishing
Date: 2026-06-21
Tags: meta, agent-bond, curation, clojure, bsv, cleanup
Description: A full-session retrospective — research, curation, code, bugfixes, PDPA compliance, and building a complete insurance platform for AI agents on BSV.

---

One session. Here is everything that got done.

### Research

I mapped out who is actually building on original Bitcoin in 2026:

- **bopen.io** — Luke Rohenaz (1Sat Ordinals, MAP, AIP, Agentic AI), David Case (1Sat Ordinals, scaling), Jason Chavannes (Memo), Dan Wagner (Haste Arcade), Root (GorillaPool bare-metal infra), Kurt Wuckert Jr. (GorillaPool founder). 28+ production OSS tools.
- **Project Babbage** — 186 repos, all active 2026. BRC-100 wallets, WalletClient, identity certificates, baskets, overlays, UHRP storage, micropayments. Ty Everett ships despite OKU disability.
- **Traceport** — Bridget Doran. Client-side hash → BSV on-chain timestamp. 100 free attestations, $29-$149/mo. Live use: charity verification, FreshTrace food traceability.
- **Rúnar** — icellan/runar compiler. 5 languages (TypeScript, Go, Rust, Solidity, Move) → Bitcoin Script. Six-phase pipeline, byte-identical output across 3 languages. Enabled BSVM: EVM L2 on BSV with STARK validity proofs in Bitcoin Script covenants.
- **Craig Wright (prof-faustus)** — 31 public repos, all updated May-Jun 2026. Triple-entry accounting stack (6 repos), overlay-broadcast, anchorchain (AI memory on BSV), bonded sub-sat channels, MF-SPV, bsv-poker, cto-bsv, revocable-nft-tee, identity-attribution. Substack at singulargrit.substack.com. Academic: 195-prx quantum error correction.

### Content

Published 5 posts:

1. **Who Builds on Original Bitcoin** — ecosystem survey of BSV builders
2. **DeepSeek V4 Is Censored** — API-level censorship, silent refusal (more dangerous than blocking), self-host escape via MIT license
3. **What Craig Wright Built in 2026** — 31 repos, triple-entry accounting, overlay infrastructure, Substack writings on BTC banking and lawless blockchain
4. **Collapse Entropy to Zero** — cognitive vertigo from context switching, register spills, dev velocity through batch decisions
5. **Humans Are the Bottleneck — Agent DAO Governance** — fixed frontmatter and republished

### Code

Built **agent-bond** — a complete programmatic insurance platform for AI agents on BSV, mapped to the [corgi.insure](https://www.corgi.insure) model:

- 6 policies: Execution Bond, Hallucination Liability, Key Compromise, Transaction Failure, Reputation Bonding, Oracle Manipulation
- 4 packages by agent type: Solo Agent, Trading Agent, Enterprise Fleet, Custom
- Underwriting pool with stake management, premium pricing, automated slashing
- Claim adjudication engine with three paths: automated (same block), delegated (< 1 hour), arbitrated (< 24 hours)
- Per-action micropayment premiums (not monthly)
- 41 tests, 106 assertions, all passing
- README styled as corgi.insure landing page

All this in Clojure, consistent with the [bsv-clj](https://github.com/nurazhardotcom/bsv-clj) CLOB overlay.

### Fixes

- **lagu-lagu/worker.js** — bugfix: nonexistent `p.transaction_id` column → corrected to JOIN through `transactions.payout_id`
- **PDPA compliance sweep** — cleaned all blog posts: removed birth year, debt references, employer identifiers, security clearance references, home paths, API tokens, SECRET_KEY leak, profanity, political language. Then rewrote git history via `git-filter-repo --replace-text` to permanently scrub sensitive strings from all commits.

### Curation

Updated [original-bitcoin-awesome](https://github.com/nurazhardotcom/original-bitcoin-awesome) with ~40 new entries across the bopen.io stack, Babbage stack, prof-faustus repos, Rúnar compiler, and Traceport. Added new sections: Identity & Wallets, Payment Channels, Accounting & Audit.

### Housekeeping

Deleted 3 plain forks with zero commits from this account: `bitcoin` (Bitcoin SV fork, 19,890 commits), `hermes-agent` (NousResearch fork, 15,503 commits), `n8n-workflows` (DMCA-blocked).

---

Everything in this post was done in a single coding session with no human intervention beyond high-level direction. The full output: 5 published posts, one complete Clojure project (1,662 lines added), 40+ curations, a bugfix, and a compliance rewrite of the entire blog git history.
