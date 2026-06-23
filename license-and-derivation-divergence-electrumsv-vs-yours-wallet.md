Title: The License & Derivation Divergence: Why My Mnemonic Didn't Import to Yours Wallet
Date: 2026-06-15
Tags: bitcoin, wallet, open-source, licensing, electrum, bip39
Description: A deep dive into why ElectrumSV mnemonic seeds fail to import into modern BIP-39 wallets like Yours Wallet, examining the underlying cryptographic differences and the legal implications of the Open BSV License.

---

Yesterday, I tried importing a seed phrase generated in my ElectrumSV desktop client (modeled in `nurazhardotcom/Bitcoin-Wallet`) into **Yours Wallet**, a modern Chrome extension designed for BSV and 1Sat Ordinals. 

The wallet immediately threw a generic validation error: **"Invalid Mnemonic!"**

As a developer, this sent me down a rabbit hole into the codebase of both wallets to find out what was going on. Is Yours Wallet broken? Was it a simple misconfiguration? Or is there a fundamental architectural divergence?

Here is what I uncovered about the cryptographic standards, derivation path designs, and the hidden licensing risks governing these two platforms.

---

## 1. Mnemonic Divergence: Electrum Seeds vs. BIP-39

The most basic reason an ElectrumSV seed phrase won't load in Yours Wallet is that **they speak completely different mnemonic languages**.

Most modern cryptocurrency wallets use the **BIP-39** standard. It uses a fixed English wordlist of 2048 words. To check if a seed phrase is valid, it computes a checksum based on the entropy bytes. 

In `yours-wallet` under `src/utils/keys.ts`, we see standard validation using the npm `bip39` package:

```typescript
if (validMnemonic) {
  const isValid = bip39.validateMnemonic(validMnemonic);
  if (!isValid) throw new Error('Invalid Mnemonic!');
}
```

However, **Electrum (and by extension ElectrumSV) explicitly does not use BIP-39.** 

Electrum created its own custom mnemonic seed format before BIP-39 existed, and they chose not to adopt BIP-39 for several architectural reasons:
1. **Self-Identifying Seed Versions:** Electrum seeds include a prefix (encoded into the hash of the words) that identifies what kind of wallet (standard, SegWit, 2FA) and derivation path the seed requires. 
2. **No Checksum Dependency:** BIP-39 requires a wordlist checksum, which Electrum developers argue makes internationalization and custom wordlists fragile.

Because of this, an Electrum seed phrase imported into a BIP-39 validator fails the checksum and gets flagged as an `"Invalid Mnemonic!"` before any keys can even be derived.

---

## 2. The Derivation Path Clash

If you bypassed the mnemonic constraint and used a BIP-39 seed in both clients, you would run into another surprise: **different default derivation paths.**

* **ElectrumSV** (under BIP-44 mode) maps its receiving addresses onto the standard receiving index chain:
  $$\text{m/44'/236'/0'/0/x}$$
* **Yours Wallet**, however, maps its default wallet address to the change chain:
  $$\text{m/44'/236'/0'/1/0}$$

To bridge this, Yours Wallet actually includes a legacy sweep service under `src/services/Keys.service.ts` that fires on initialization:

```typescript
const sweepWallet = generateKeysFromTag(keys.mnemonic, "m/44'/236'/0'/0/0");
// Query and sweep any UTXOs sitting at the old default path
```

If it detects funds on the standard `m/44'/236'/0'/0/0` path, it automatically sweeps them into the change address path Yours Wallet uses. But this sweep only runs *after* a valid BIP-39 mnemonic is successfully decrypted—something an Electrum seed cannot achieve.

---

## 3. The Licensing Trap: MIT vs. Open BSV License

Beyond cryptography, the codebase comparison revealed a significant legal divergence that every developer in the space should be aware of.

### Yours Wallet: Permissive MIT
Yours Wallet is licensed under the **MIT License**. This is a highly permissive, standard open-source license. You can fork it, write custom extensions, build commercial closed-source apps, and launch it on any network (BTC, BSV, BCH, MVC, etc.) without fear of legal recourse, provided you retain the copyright header.

### ElectrumSV: The "Open BSV" Constraint
ElectrumSV is built on a hybrid codebase. While it inherits MIT-licensed code from Electrum and Electron Cash, its proprietary and net-new features are covered by the **Open BSV License**:

> *"The Software, and any software that is derived from the Software or parts thereof, can only be used on the Bitcoin SV blockchains."*

This network-locking clause makes the license **non-compliant with the Open Source Definition (OSD)** managed by the Open Source Initiative (OSI) because it discriminates against other networks (violating Clause 6). 

From a developer standpoint:
* If you fork ElectrumSV and attempt to repurpose the codebase for a BTC, LTC, or BCH wallet, you are **violating the copyright license** and exposing your project to litigation.
* If you fork Yours Wallet, you are legally free to deploy it on any network.

---

## The Verdict

If you have legacy wallets in ElectrumSV and want to move to Yours Wallet, the solution isn't to import your seed phrase. Instead:
1. **Export the raw private key (WIF format)** from ElectrumSV for your specific address.
2. Go to Yours Wallet -> **Settings** -> **Accounts** and import the account using the private key (WIF).
3. Or simpler: perform a standard on-chain transaction to transfer the balance to a freshly generated Yours Wallet address.

For developers seeking a clean, legally unencumbered foundation for multi-chain wallet development, **Yours Wallet's MIT-licensed codebase** is the clear choice.
