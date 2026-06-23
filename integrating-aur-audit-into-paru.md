Title: Hardening the Arch/CachyOS Build Pipeline: Native AUR Auditing with Paru & Babashka
Date: 2026-06-14
Tags: security, linux, archlinux, devops, systems
Description: A visual reference guide to native PreBuild hooks. We configure paru to automatically run our Clojure-based aur-audit scanner before compiling community packages.

---

When updating an Arch-based system (like CachyOS) via AUR helpers, we are executing arbitrary build scripts (`PKGBUILD` and `.install` scripts) written by community members. During supply chain incidents, relying on manual human checks is a massive liability.

This post serves as a high-signal reference to automate static analysis on these scripts natively before compilation.

---

### The Architecture: Before vs. After

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    subgraph LegacyWorkflow["Risky: Before aur-audit"]
        A1["paru install google-chrome"]
        A2["Fetch AUR Git Repository"]
        A3["Human Review / Pager"]
        A4["Execute PKGBUILD / makepkg"]
        subgraph A5["System Compromised"]
        end
        A2 --> A3
        A3 -->|"User presses q / confirms"| A4
        A4 -->|"Malicious code runs on host"| A5
    end
    subgraph SecureWorkflow["Hardened: After aur-audit"]
        B1["paru install google-chrome"]
        B2["Fetch AUR Git Repository"]
        B3["Trigger: PreBuildCommand"]
        subgraph B4["aur-audit.clj Scan"]
        end
        subgraph B5["Abort Build & Halt execution"]
        end
        B6["Execute PKGBUILD / makepkg"]
        subgraph B7["Secure Installation"]
        end
        B2 --> B3
        B3 --> B4
        B4 -->|"Exit Code: 1 / Threat Found"| B5
        B4 -->|"Exit Code: 0 / Clean"| B6
        B6 --> B7
    end
```

---

### Security Execution Matrix

| Metric | Before Integration | After Integration |
| :--- | :--- | :--- |
| **Verification Gate** | Manual scrolling (Human eye) | Automated regex parser + human backup |
| **Halt Condition** | User notices anomaly and presses Ctrl+C | Exit Code `1` from `PreBuildCommand` kills loop |
| **Verification Speed** | Slow, prone to fatigue | Instant (Milliseconds via Babashka) |
| **Audit Scope** | Only files shown in the pager | Deep scan of both `PKGBUILD` and `.install` scripts |
| **Vulnerability Signatures** | None | Scans for curl/wget payloads, systemd persistence, profile injections |

---

### System Integration Commands

To configure this setup, we link our scanner to our local userspace path and hook it directly into `paru`'s execution engine.

#### 1. Symlink and Make Executable
We place the compiler/script within our local userspace bin directory:

```fish
ln -sf ~/ Documents/Bugs/aur-audit/aur-audit.clj $HOME/.local/bin/aur-audit
chmod +x $HOME/.local/bin/aur-audit
```

#### 2. Native Paru Hook configuration
Append the pre-build trigger configuration to your local `paru.conf` file:

```ini
# ~/.config/paru/paru.conf
[options]
PreBuildCommand = ~/ .local/bin/aur-audit
```

---

### Threat Scanner Flow

Whenever a build begins, the pre-build phase executes the following logic inside the package source directory:

```mermaid
%%{init: {'theme': 'neutral', 'themeVariables': {'primaryColor': '#f5f5f5', 'primaryTextColor': '#333', 'primaryBorderColor': '#ccc', 'lineColor': '#555', 'secondaryColor': '#e8e8e8', 'tertiaryColor': '#fafafa'}}}%%
flowchart TD
    subgraph initial["initial"]
    end
    ReadTargets["ReadTargets"]
    initial --> ReadTargets
    ReadTargets["ReadTargets"]
    ScanPKGBUILD["ScanPKGBUILD"]
    ReadTargets -->|"Locate PKGBUILD"| ScanPKGBUILD
    ReadTargets["ReadTargets"]
    ScanInstallScripts["ScanInstallScripts"]
    ReadTargets -->|"Locate *.install files"| ScanInstallScripts
    CheckNetwork["CheckNetwork"]
    CheckObfuscation["CheckObfuscation"]
    CheckNetwork -->|"Regex matches base64/eval/openssl"| CheckObfuscation
    CheckObfuscation["CheckObfuscation"]
    CheckPersistence["CheckPersistence"]
    CheckObfuscation -->|"Regex matches systemd/cron/profile"| CheckPersistence
    ScanPKGBUILD["ScanPKGBUILD"]
    EvaluateResults["EvaluateResults"]
    ScanPKGBUILD --> EvaluateResults
    ScanInstallScripts["ScanInstallScripts"]
    EvaluateResults["EvaluateResults"]
    ScanInstallScripts --> EvaluateResults
    HighRiskFound["HighRiskFound"]
    SystemExit1["SystemExit1"]
    HighRiskFound -->|"Exit Code 1 (paru halts build)"| SystemExit1
    Clean["Clean"]
    SystemExit0["SystemExit0"]
    Clean -->|"Exit Code 0 (paru compiles package)"| SystemExit0
```

With this integration, you establish a system-enforced defense gate that eliminates cognitive fatigue during package upgrades.
