Title: What Craig Wright Built in 2026
Date: 2026-06-21
Tags: bitcoin, bsv, craig-wright, prof-faustus, triple-entry, accounting, overlay, runar
Description: Craig Wright's 31 GitHub repos — all updated in May-June 2026 — reveal a coherent engineering push: triple-entry accounting, overlay infrastructure, AI agent memory, mental poker on Bitcoin Script, and the Rúnar compiler.

Craig Wright writes a lot. The question has always been whether "a lot" means productive or just prolific. In 2026, the answer is becoming clear.

His GitHub profile is prof-faustus. His bio: "Polymath. Multiple PhDs. I read the sources, follow the logic, and dismantle the slogans — credentials are real; outrage is not."

He has 31 public repositories. Every single one of them was updated between May 27 and June 18, 2026. That is not the pattern of someone who has given up. That is the pattern of someone who has found a second wind.

## The Triple-Entry Accounting Stack

The most substantial body of work is a complete triple-entry accounting system built on BSV.

**triple-entry-bsv-sql** (21 stars) — A forked-PostgreSQL triple-entry ledger with tokenisation and EDI/logistics integration. End-to-end regtest testing. Go. This is not a toy. It is a commercial-grade accounting system with Bitcoin-native settlement.

**triple-entry-evidence-bsv** (8 stars) — BSV-native TEA evidence: hierarchical keys, ECDH linkage, per-field commitments, scoped disclosure. Rust.

**triple-entry-evidence** (5 stars) — The reference implementation in Python.

**verifiable-accounting-bsv** (6 stars) — Audit evidence system with per-field selective disclosure over an intra-transaction Merkle field tree.

**verifiable-accounting-chain** (5 stars) — PKI-rooted, ECDH-linked, spend-linked transaction chain of triple-entry records with general ledger and tax reporting.

**verifiable-accounting** (6 stars) — Verifiable Accounting Arithmetic Without Disclosure. An IJAIS submission using Bulletproofs on secp256k1.

Six repos, one coherent system: triple-entry accounting with selective disclosure, anchored on BSV, with academic publication. This is the kind of infrastructure that enterprise audit firms would need if they ever adopted blockchain. Whether they will is another question, but the engineering is done.

## Infrastructure

**overlay-broadcast** (18 stars) — Rust overlay broadcast service. The most-starred infrastructure repo. Reference implementation for overlay topic management and transaction propagation on BSV.

**memserve** (5 stars) — In-memory, hash-sharded transaction lookup fabric over Teranode. Seen/Mined/Merkle-path/UTXO from memory. Pay-per-use BSV payment channels with spend-depth pruning.

**mfspv** (2 stars) — Merkle-Forest SPV. Sender-held inclusion proofs for BSV/Teranode targeting 10^6 to 10^11 transactions per second. Reference implementation with security audit and publication-grade evaluation. Go.

**merkle-service** (2 stars) — Fork of the official BSV merkle-service. High-throughput Merkle proof delivery, interoperable with Teranode and Arcade.

**bonded-subsat-channel** (10 stars) — Bonded sub-satoshi channels on BSV. Reference implementation with standalone embedded node, wallet, and watchtower. Enables payments below 1 satoshi — essential for AI agent micropayments.

## AI and Identity

**anchorchain** (14 stars) — Immutable referencing of AI memory states through BSV blockchain-indexed volumetric data linking. Includes file linking, selective disclosure, an AI credit system, and a hardened metering authority. This is the AI agent memory layer — timestamped, verifiable, and permanent.

**identity-attribution** (5 stars) — SCARCITY identity attribution: indefinite-scale sparse-Merkle registry + zero-knowledge proofs + federated BSV anchoring + Docker service deployment. Rust.

**idattr-onchain** (4 stars) — Anchor identity-registry roots on the BSV chain via BIP143/FORKID-signed transactions.

**tee-sim** (4 stars) — Simulated TEE/secure element: non-exportable device key, attestation quotes, presentation binding. Wire-compatible with idattr-device.

## Games as Protocol Research

**bsv-poker** (15 stars) — Dealerless, non-custodial multiplayer poker on BSV. Web + Windows desktop, real BSV node, bonded sub-sat channel. Built from specification. Also has a Blazor WebAssembly web version (bsv-poker-web), and Chinese translations (bsv-poker-zh, mental-poker-zh, cardtable-zh).

**cardtable** (7 stars) — Dealerless distributed card-game protocol. Transaction-native state machine.

**estates** (8 stars) — Dealerless, fully-on-chain, fully-auditable property game with 1-sat NFT deeds. C#.

These are not just games. They are reference implementations for state machine protocols on Bitcoin Script. The poker code demonstrates mental poker — a cryptographic protocol for dealerless card games that Satoshi cited as an inspiration for Bitcoin.

## Developer Tooling

**bsv-universal-sdk** (6 stars) — Universal BSV game/contract engine. Secure-by-construction, dealerless, non-custodial. TypeScript.

**nft-wallet-bsv** (8 stars) — Non-custodial BSV full-Script wallet with encrypted-NFT atomic swap. Go. "BSV only, no OP_RETURN" — meaning everything done in pure Script.

**cto-bsv** (8 stars) — TypeScript BSV development toolkit.

**revocable-nft-tee** (8 stars) — Revocable TEE-sealed, forward-revocable encrypted NFT on BSV-native. Rust.

**tea-package** (7 stars) — Python TEA package.

## Academic Work

**195-prx** (1 star) — Conditional, falsifiable study of when surface-code distance-suppression collapses (common-mode noise). Theory + GPU simulation. Quantum error correction research.

**M840** (5 stars) — Spectral stability analysis of weighted complete graph Laplacians under structured perturbations. Focus on algebraic connectivity, Rayleigh quotient bounds, cut-capacity bottlenecks. TeX. Mathematics.

**sdi143** (1 star) — Replication package for JIS-2026-016: "Compliance Theater in AI-Integrated Reporting." An academic paper about how companies fake AI compliance.

## The Writing

Beyond the code, Wright is publishing actively on Substack at singulargrit.substack.com. Recent posts from June 2026 alone:

**"BTC Is Banking with Extra Steps"** (June 18) — The core argument: BTC diagrams show Alice paying Bob through Square, Lightning, nodes, Coinbase. Bitcoin was designed for Alice to transact IP-to-IP. "If a system cannot handle a direct payment, it is not digital cash."

**"The Warehouse and the Mind"** (June 18) — Analysis of UK property law as applied to digital assets. Covers AA v Persons Unknown [2019], Tulip Trading v van der Laan [2023], and the Property (Digital Assets etc) Act 2025. The argument: crypto-tokens are rivalrous by design and therefore property under English law.

**"The Lawless Blockchain Is a Story We Tell for Small Change"** (June 15) — Based on his paper "Legal Deterrence in 'Permissionless' Consensus" (IJCR, Vol 6, 2026). Analysis of 4,149 blocks from March 2026: US-linked pools account for 42% of hash, China-linked ~5%, F2Pool 11%. More than 95% attributable to identifiable operators. Conclusion: the "permissionless" story only holds for transactions below ~$1-4 million.

**"The Arithmetic of the Last Fool"** (June 12) — BTC as Ponzi dynamics. "The future belongs to systems that move economic reality, not systems that sneer at it."

**"The Beast at the Door"** (June 6) — How ETF institutionalisation creates a downside profit machine. If an institution controls a large visible BTC position while holding synthetic short exposure, a decline to $30,000 can produce ~$90 billion in gross profit at a 10x exposure multiple.

**"The Miner Is Not a Monarch"** (May 20) — NAR/DAR legal-economic analysis. Miners are commercial entities within legal jurisdictions, not sovereigns. Compliance with valid court orders is not governance — it is legal obligation.

**"What Siggi Built"** (April 22) — The Rúnar compiler and BSVM. Covers the technical architecture, the FRI verifier covenant, the six-phase pipeline, and the significance of writing a STARK verifier in Bitcoin Script.

## The Pattern

The 31 repos, the Substack output, the academic papers — they paint a picture of someone who has stopped trying to convince the public and focused entirely on building. The accounting stack, the identity infrastructure, the layer 2 research, the legal analysis. These are not the moves of a showman. They are the moves of someone engineering a parallel financial system.

Whether you believe Wright is Satoshi is irrelevant to this observation. The output is measurable. The repos compile. The papers are peer-reviewed. The code runs on regtest and mainnet.

Judge the work, not the man.