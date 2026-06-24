Title: How I Curated an Awesome List of Projects Building on Original Bitcoin
Date: 2026-06-21
Tags: curation, bsv, awesome-list, ecosystem, meta
Description: The process, criteria, and research methodology behind original-bitcoin-awesome — a living directory of 2026-active projects on BSV.

---

[original-bitcoin-awesome](https://gitlab.com/nurazhar/original-bitcoin-awesome) is a curated list of projects, tools, and infrastructure built on the original Bitcoin protocol (BSV). It started as a simple README. It has grown to 216+ lines covering nodes, SDKs, wallets, smart contracts, identity, payment channels, accounting, and agent infrastructure.

This post explains how it is curated.

## Scope

The list is deliberately narrow. It covers only the original Bitcoin protocol — defined as Satoshi's v0.1 design preserved by BSV after the Chronicle upgrade (block 943,816, April 7, 2026). No BTC, no BCH, no forks that deviated from the whitepaper.

Within that scope, I include any project that:
- Builds directly on BSV infrastructure (nodes, SDKs, wallets, overlays)
- Integrates with BSV for some function (timestamping, payments, data)
- Produces tools that make BSV usable (indexers, explorers, compilers)
- Is actively maintained in the current calendar year (2026)

## Research Process

Finding active projects is harder than it should be. GitHub search is noisy. Many BSV projects are scattered across personal accounts, not orgs. The research process:

1. **GitHub org spelunking** — Start with known orgs (bitcoin-sv, p2ppsr, gorillapool, bopen, prof-faustus). Walk every repo, check last commit date, read the README.

2. **Twitter/X feed scanning** — Follow BSV developers. When someone links a repo, check if it is active and relevant.

3. **Developer-to-developer discovery** — One interesting repo leads to its dependencies, which lead to other repos, which lead to other developers. This is how I found Rúnar (icellan/runar), Traceport, and the Indelible federation.

4. **Commit freshness filter** — If the most recent commit is older than 12 months, the project is excluded regardless of quality. The BSV ecosystem moves fast in 2026; stale projects signal abandoned effort.

## What Has Been Curated

As of this writing, the list covers:

| Section | Entries | Highlights |
|---------|---------|------------|
| Nodes & Infrastructure | 10 | SV Node, Teranode, GorillaPool, Junglebus, BitIndexer |
| SDKs | 13 | bsv.js, go-bsv, bsv-clj, bsv-universal-sdk, Rúnar compiler |
| Wallets | 8 | Yours Wallet, mintBlue, Babbage Wallet, Clawsats |
| Smart Contracts | 6 | sCrypt, Sensible Contract, MAP, AIP, TSC |
| Identity & Wallets (BRC) | 8 | BRC-31, BRC-77, Sigma Identity, Clawsats identity |
| Payment Channels | 4 | Bonded sub-sat channels, pay-to-IP, SPV channels |
| Accounting & Audit | 11 | Triple-entry BSV stack, verifiable accounting (6 repos) |
| Agent Infrastructure | 6 | Indelible, Clawsats, Anchorchain, identity-client |
| Applications | 7 | BSV Poker, ESTATES, Memo, Haste Arcade, MNEE |
| Tooling | 12 | ORDFS, BitBench, BigBlocks, Nodeless, Teranode CLI |
| Academic & Research | 3 | 195-prx, SDI 143, Verifiable Accounting IJAIS |

## Geographic Spread

The BSV builder ecosystem in 2026 is physically distributed:

- **North America**: Luke Rohenaz (bopen.io), Ty Everett (Babbage), Jason Chavannes (Memo), Dan Wagner (Haste Arcade), Root (GorillaPool), Kurt Wuckert Jr.
- **Europe**: Bridget Doran (Traceport, UK), icellan (Rúnar compiler, EU), nChain (London)
- **Asia-Pacific**: GorillaPool relay (Japan/Singapore), Indelible bridge in Sydney
- **Global**: prof-faustus repos, various individual contributors

## What Gets Excluded

Some projects are deliberately omitted:

- **BTC/BCH forks** — Not original Bitcoin protocol
- **Speculation-focused** — Only utility and technology
- **Stale projects** — No commits in 2026
- **Proprietary closed-source** — Cannot verify claims
- **Copies without additions** — A repo that just mirrors another project without its own contribution

## How to Add a Project

The list accepts PRs and issues with a simple bar: demonstrate 2026 activity. Open a PR or issue at `gitlab.com/nurazhar/original-bitcoin-awesome`.

---

The ecosystem is growing faster than I can track. If you know of an active project building on original Bitcoin that is not listed, tell me.
