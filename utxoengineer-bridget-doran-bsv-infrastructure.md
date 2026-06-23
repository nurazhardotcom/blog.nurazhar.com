Title: utxoengineer.com — The Independent BSV Engineer Shipping Production Infrastructure
Date: 2026-06-21
Tags: bitcoin, bsv, teranode, infrastructure, bridget-doran, traceport, open-source
Description: Bridget Doran has built the first live Teranode block explorer, a data attestation API (Traceport), BRC-77 identity demos, and libp2p gossipsub tooling — all as an independent BSV engineer. A look at the infrastructure she ships and what it tells us about the ecosystem.

---

The BSV ecosystem in 2026 is often described through its institutions — the BSV Association, nChain, the Teranode team at bsv-blockchain. These organizations build the reference implementations and standards.

But the real signal of a healthy protocol ecosystem is **independent engineers shipping production infrastructure without institutional backing.**

Bridget Doran ([utxoengineer.com](https://utxoengineer.com)) is that signal. She runs her own DevOps on Google Cloud, publishes open-source BSV tooling, publishes the daily BSV Intel Report, and has built the first live Teranode block explorer. Her work is worth studying because it reveals what the protocol makes possible for a single engineer.

---

## The Teranode Explorer

[explorer.utxoengineer.com](https://explorer.utxoengineer.com) is the first live observability tool for BSV's Teranode gossip layer. It visualizes:

- **Blocks** — real-time block propagation across the Teranode mesh
- **Subtrees** — Teranode's parallel block production subtrees
- **Operators** — active Teranode operator nodes and their connectivity
- **Rejects** — rejected transactions and blocks with reasons

This is not a standard block explorer. Standard explorers show confirmed transactions at rest. The Teranode explorer shows the network *in motion* — how blocks propagate through the libp2p gossipsub mesh before they're confirmed. It makes visible a layer of the protocol that was previously opaque.

The companion repository [bsv-teranode-listener](https://github.com/HBGnostic/bsv-teranode-listener) is a working reference implementation for connecting to BSV Teranode's mainnet gossipsub layer. Any developer can use it to build their own observability or monitoring tooling.

---

## Traceport: Data Attestation on BSV

[Traceport](https://traceport.io) is Bridget's production API for business data attestation on BSV. It turns real-world records into verifiable on-chain receipts.

The use cases are concrete:

- **Traceport for Charities** ([traceport.io/charities](https://traceport.io/charities)) — donation tracking and impact verification
- **FreshTrace** ([freshtrace.io](https://freshtrace.io)) — food supply-chain traceability prototype (FSMA 204 compliance)

These are not toy dApps. Traceport is a production API running on Google Cloud (Cloud Run, Pub/Sub, long-running VMs) that businesses can integrate with. It demonstrates a pattern that will define BSV's utility value proposition: **attestation of real-world data via OP_RETURN**, with overlay indexing for efficient query.

---

## BRC Standards Demos

Bridget has also shipped several educational demos of BRC standards:

- [brc100-encrypted-messaging](https://github.com/HBGnostic/brc100-encrypted-messaging) — encrypted messaging using BRC-100 wallet methods (encrypt, decrypt, createSignature, verifySignature)
- [BRC100-Visualization](https://github.com/HBGnostic/BRC100-Visualization) — visualization tool for BRC-100 token state

These matter because BRC standards are the interoperability layer of the BSV ecosystem. Production tooling depends on developers understanding how to use them. Open-source demos lower the barrier to entry.

---

## What This Tells Us

A single independent engineer shipping this range of infrastructure — a Teranode mesh explorer, a production attestation API, a Telegram-based daily intel report, multiple BRC standard demos — is evidence that the protocol removes rather than creates friction.

Compare with other ecosystems where shipping a production application requires:

- Running or renting a validator node
- Paying for gwei-level gas on each transaction
- Managing RPC endpoint availability
- Navigating complex bridge infrastructure

Bridget runs her own VM, connects directly to Teranode's gossipsub layer, pays sub-cent fees, and has no dependency on third-party API providers. The protocol's simplicity (UTXO model, OP_RETURN, direct P2P) makes this feasible for a team of one.

---

## Summary

- Bridget Doran is an independent BSV engineer shipping production infrastructure
- First live Teranode block explorer — real-time mesh observability
- Traceport — production data attestation API on BSV
- Open-source BRC standards demos
- BSV Intel Report — daily ecosystem intelligence via Telegram
- All built without institutional backing, on Google Cloud, connected directly to the BSV P2P network

Her work is the strongest signal yet that BSV's "utility layer" thesis is moving from argument to demonstration.
