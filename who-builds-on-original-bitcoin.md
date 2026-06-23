Title: Who Builds on Original Bitcoin
Date: 2026-06-21
Tags: bitcoin, bsv, bopen, babbage, traceport, runar, ecosystem, developers
Description: A deep dive into the builders shipping code on BSV in 2026 — bopen.io, Project Babbage, Traceport, Rúnar, and the infrastructure they produce.

If you follow Bitcoin development, you have probably heard "nobody builds on BSV." The people who actually build on it might disagree.

bopen.io is a consultancy in the BSV ecosystem. Recently they put up a team page that gives a rare look at who is actually writing code on the original protocol. The names are worth knowing because their work is the infrastructure that the rest of the ecosystem depends on.

## The bopen.io Team

**Luke Rohenaz** (@rohenaz) — Architect of the applications and primitives that define the blockchain ecosystem. He is now integrating Agentic AI into the developer stack — engineering workflows across Claude, GPT, and Gemini for autonomous protocol and app development. He co-created 1Sat Ordinals and authored foundational standards like MAP (Message Access Protocol) and AIP (Application Interface Protocol). If you use overlays on BSV, you use Luke's work.

**David Case** (@shruggr) — Co-created 1Sat Ordinals and 1sat.market with Luke. Built the infrastructure for CryptoFights, a game that at its peak processed more daily transactions than many Layer 1 networks combined. He has a 1sat-indexer, maintains Teranode and TS-stack forks, and generally stress-tests everything at scale.

**Jason Chavannes** (@jchavannes) — Bitcoin OG. Founded Memo, the first on-chain social network. He was shipping decentralized social media years before it became an industry buzzword. His work has shaped how the ecosystem approaches permanent, user-owned data.

**Dan Wagner** (@danwag06) — Founder of Haste Arcade (instant-payout gaming). Specialist in delivery management and product design. Built react-onchain for deploying web apps to the BSV blockchain.

**Root** (@GorillaPool) — The bare-metal infrastructure specialist. Running servers and mining blocks from his garage for years. Key player in the Teranode implementation for GorillaPool. When you need infrastructure that does not rely on cloud abstractions, Root builds it from the ground up.

**Kurt Wuckert Jr.** — You know him from media appearances. Decades of IT and cybersecurity experience, 10+ years in blockchain infrastructure. Founder of GorillaPool. Primary point of contact for clients building on BSV.

## What They Have Shipped

The team page lists 28+ open source projects. A few that matter:

**1Sat Ordinals** — Protocol for inscribing data onto individual satoshis. The base layer for digital artifacts on original Bitcoin.

**MAP (Message Access Protocol)** — Addressing, routing, and retrieving messages on the blockchain. The communication layer for overlay networks.

**AIP (Application Interface Protocol)** — Application-layer interoperability. Makes different BSV services talk to each other.

**Bitcoin Schema** — Structured on-chain data. The schema framework that gives meaning to raw OP_RETURN outputs.

**ORDFS** — Overlay file system for decentralized storage. A decentralized file system that lives on the overlay network.

**Nodeless** — Payment processing infrastructure. The kind of thing that makes BSV actually usable for commerce.

**BigBlocks** — Block explorer. BitBench — benchmarking. BitPic — profile pictures linked to Bitcoin keys. DropLit — token distribution. Jamify — UTXO management. Sigma Identity — certification. ThemeToken — token-gated theming. BitChat — encrypted messaging. Haste Arcade — instant payout gaming. Yours Wallet — content monetization. MNEE — fiat-backed stablecoin.

All of these are active in 2026.

## Project Babbage in 2026

Project Babbage (founded by Ty Everett, covered in a previous post) has been quietly shipping through 2026. Their GitHub organization at github.com/p2ppsr has 186 repositories — all showing updates in May-June 2026.

The Babbage stack today:

**BRC-100 Wallets** — Permissioned app-to-wallet requests for identity, keys, certificates, and payments. The wallet is the user's agent — apps ask for permission, not the other way around.

**user-wallet** — The reference BRC-100 wallet implementation. Active development.

**peerpay** — React frontend for BRC-100 peer-to-peer payments.

**lars** — Local Automated Runtime System for BSV application development.

**locksmith** — Hodlocker for time-locking BSV tokens for a number of blocks.

**the-bitcoin-cpu** — Execute code with the Everett CPU architecture on Bitcoin. A novel computing model using Bitcoin Script.

**tempo** — Music streaming and publishing platform.

**MetaMarket** — BSV marketplace.

**convo-messenger** — Secure messaging using the Babbage stack.

**bsv-vault-manager-suite** — Vault management for BSV.

The Babbage docs at docs.projectbabbage.com are the best entry point for building BSV apps with user-held keys. The stack now covers: wallet auth, baskets (user-local UTXO state), identity certificates, overlay networks, UHRP storage, micropayments, and messaging.

## Traceport: Private Data, Public Proof

Traceport (traceport.io) is a business data attestation API built by Bridget Doran. The model is simple: you submit data, they hash it client-side, and the hash goes on the BSV blockchain. No one sees your data but you. When you need to prove a record exists — for audits, disputes, or compliance — you share the original data, and anyone can verify it matches the blockchain proof.

Pricing: 100 free attestations, then $29-$149/mo. Live use cases include charity verification and food traceability (FreshTrace).

Traceport is a commercial product — the first I have seen from the BSV ecosystem that has a straightforward pricing page, a dashboard, and a clear value proposition for non-blockchain businesses. "Make data provable, not just stored."

## Rúnar: Write Any Language, Compile to Bitcoin Script

Siggi's Rúnar compiler (github.com/icellan/runar) is a piece of infrastructure that changes what is possible on BSV. It lets developers write smart contracts in TypeScript, Go, Rust, Solidity, or Move — and compiles them to Bitcoin Script with formal correctness guarantees.

The compiler went through a six-phase pipeline, was evaluated against 50 contracts in the test corpus, and produced byte-for-byte identical output across three languages. The BSV Association published the technical report in March 2026.

Rúnar enabled BSVM: an EVM-compatible Layer 2 that runs on BSV, where every state transition is authorized by a STARK validity proof verified inside a Bitcoin Script covenant. That covenant was compiled by Rúnar. Without Rúnar, hand-writing 85 KB of bug-free Script for a FRI verifier is not a realistic ask of any human.

## What This Adds Up To

The narrative that "nobody builds on BSV" does not survive contact with the actual repositories. bopen.io alone has shipped two dozen production tools. Project Babbage maintains 186 repos, all active. Traceport is selling a commercial product. Rúnar is producing academic-grade compiler infrastructure.

The builders are not in it for speculation. They are building infrastructure for a utility network — the original Bitcoin protocol as a settlement and data layer. Whether that thesis is correct remains to be seen. But the claim that nobody is trying is no longer true, if it ever was.

Project links:
- bopen.io — https://bopen.io
- Project Babbage — https://projectbabbage.com
- Traceport — https://traceport.io
- Rúnar — https://github.com/icellan/runar
- BSVM — Craig Wright's Substack (singulargrit.substack.com), "What Siggi Built" (April 2026)