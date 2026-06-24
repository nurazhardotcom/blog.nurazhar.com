Title: How I Fixed a Broken AUR Install in 2 Commands
Date: 2026-06-19
Tags: arch-linux, aur, pgp, troubleshooting
Description: Using aur-audit to diagnose missing PGP keys blocking wkhtmltopdf-bin installation on Arch.

---

I tried installing `wkhtmltopdf-bin` from AUR. It failed. Not with some cryptic compile error or dependency hell — it failed because of **missing PGP keys**.

The thing is, I didn't know that at first. The error output from `paru` was noisy. Multiple packages needed to be built from source (`libjpeg6-turbo`, `openssl-1.1`, then `wkhtmltopdf-bin`), and somewhere in the GPG verification step things broke.

## The Tool That Made It Obvious

I used [aur-audit](https://gitlab.com/nurazhar/aur-audit) — a tool I wrote specifically for this kind of situation. You point it at your AUR package repo, and it tells you exactly what's wrong.

The output was surgical:

- `libjpeg6-turbo` → missing key `85C7044E033FDE16`
- `openssl-1.1` → missing key `D894E2CE8B3D79F5`

Two keys. That's it. That was the entire problem.

## The Fix

```bash
gpg --recv-keys 85C7044E033FDE16  # libjpeg6-turbo
gpg --recv-keys D894E2CE8B3D79F5  # openssl-1.1
```

Then retry:

```bash
paru -S wkhtmltopdf-bin
```

Both dependencies compiled cleanly. All tests passed (libjpeg-turbo's test suite printed "GREAT SUCCESS" — yes, really). OpenSSL ran 160 test recipes, 2667 tests, all passed. `wkhtmltopdf-bin` installed from the pre-built Debian binary.

Total time from failure to working install: under 5 minutes. Most of that was compile time.

## Why This Matters

Without the audit tool, the debugging process looks different. You read the error output, maybe Google the GPG failure, figure out you need to import keys, find the key IDs somewhere in the PKGBUILD or AUR comments, import them, retry. It works, but it's reactive and slow.

With `aur-audit`, the workflow is:

1. Run audit
2. Import the exact keys it tells you
3. Retry install

No Googling. No reading PKGBUILDs. No guessing.

## The General Pattern

This isn't just about `wkhtmltopdf-bin`. AUR packages that depend on other AUR packages will fail at GPG verification if the signing keys aren't in your keyring. The error messages don't always make it obvious *which* keys are missing — especially when multiple packages are being built in sequence.

The audit approach: check before you build, not after it fails.

---

*Running CachyOS (Arch-based). AUR helper: `paru`. The `aur-audit` tool is on [GitHub](https://gitlab.com/nurazhar/aur-audit).*
