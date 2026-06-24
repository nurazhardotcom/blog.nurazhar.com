Title: Bypassing the Workday & SuccessFactors ATS Bloat: A Systems Approach to Job Applications
Date: 2026-06-24
Tags: systems, careers, automation, python, typst, dns
Description: Why manual form-filling is a system error. How I built a local-first application dashboard using Pandoc, Typst, and the Resend API to apply for jobs in under 30 seconds.

---

If you are an engineer applying for jobs in 2026, you are likely hitting a massive middleware firewall. 

You find a job listing. You click apply. Instead of a simple handshake, you get redirected to a bloated legacy ATS portal (usually Workday or SAP SuccessFactors) requiring you to create an account and manually re-type your entire education, work history, and certificates into unstyled input boxes.

To an analytical mind, this performative compliance check is an infuriating waste of clock cycles. 

Instead of typing your life into database inputs over and over again, you can treat your job search like an API routing problem. Here is how I built a local-first application compiler and delivery pipeline.

---

## The Architecture of the Pipeline

The objective is to minimize cognitive friction and reduce the application submission time to under 30 seconds per role. The system consists of three modules:

```
[ Tailored Markdown ] ──► [ Pandoc + Typst ] ──► [ Local HTML Dashboard ] ──► [ Resend API ]
  (Factual Source)          (PDF Compiler)         (Application Control)       (Direct Delivery)
```

---

## 1. Compilation: Pandoc + Typst
Instead of formatting resumes in Google Docs or wrestling with CSS layouts, I write the source resume in plain Markdown (`resume.md`).

To compile this into a beautifully formatted, print-ready PDF, I leverage **Pandoc** with the modern **Typst** layout engine. 

```bash
pandoc resume.md -o tailored_resume.pdf --pdf-engine=typst
```

This compiles instantly and generates a clean layout without any manual font-tinkering or positioning anomalies.

---

## 2. Management: The Local Control Center
Instead of tracking applications in various browser tabs or spreadsheet rows, I compile a single local `apply_dashboard.html` file in my workspace.

For each target job, the dashboard displays:
1. **Match Highlights**: Crucial technical alignment notes (e.g. mapping CyberArk PAM or Microsoft Purview DLP experience directly to the JD).
2. **Action Vectors**:
   * **Open Listing**: Direct link to the submission page (prioritizing platforms supporting one-click "Easy Apply").
   * **Email Recruiter**: A pre-filled `mailto:` link containing the recruiter's address, specific subject line, and a targeted cover letter body.
   * **Copy Resume Path**: A button that copies the absolute path of the tailored PDF to the clipboard (`/home/.../resume.pdf`), allowing a simple `Ctrl+V` in the browser's file upload dialog.

---

## 3. Dispatch: Resend API + Python
For recruiters who accept applications directly via email, opening a mail client and manually attaching files is slow.

Instead, I wrote a Python script (`send_applications.py`) to dispatch the application. The script reads a **Resend API Key** from a local `.env` file, encodes the compiled PDF as a base64 string, and submits a POST request to Resend's API.

### The Cloudflare User-Agent Gotcha
While testing the Python script, Cloudflare's bot protection intercepted the default `urllib` user-agent, returning a `403 Forbidden` (`error code: 1010`).

The bypass is trivial—explicitly injecting a standard browser User-Agent header into the request:

```python
import base64
import urllib.request
import json

def send_email_with_pdf(to_email, subject, body, pdf_path, resend_key):
    with open(pdf_path, "rb") as f:
        encoded_pdf = base64.b64encode(f.read()).decode("utf-8")
        
    payload = {
        "from": "Nur Azhar <career@nurazhar.com>",
        "to": [to_email],
        "subject": subject,
        "text": body,
        "attachments": [{"content": encoded_pdf, "filename": "resume.pdf"}]
    }
    
    req = urllib.request.Request(
        "https://api.resend.com/emails",
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {resend_key}",
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."
        },
        method="POST"
    )
    
    # Execute request...
```

---

## 4. DNS Routing
To ensure the email lands in the recruiter's inbox instead of the spam folder, we must configure SPF and DKIM records on the custom domain (e.g. `nurazhar.com`). 

Resend provides unique DKIM and SPF records that must be mapped to your DNS registrar (e.g. Spaceship):
* **DKIM TXT Record**: Set up for sub-name `resend._domainkey` containing the RSA public key.
* **SPF Records**: Added to the `send` subdomain (`send.nurazhar.com`) as an MX record pointing to `feedback-smtp.amazonses.com` and a TXT record containing `v=spf1 include:amazonses.com ~all`.

Using `dig` in the terminal confirms active propagation:

```bash
$ dig send.nurazhar.com TXT
send.nurazhar.com.      60      IN      TXT     "v=spf1 include:amazonses.com ~all"
```

Once the domain transitions to `verified` status on the Resend dashboard, you can trigger email delivery instantly.

---

## Stop Typing, Start Routing
The corporate hiring middleware is designed to enforce compliance. You do not need to fight their database models manually. 

Compile your resume via markdown, organize your coordinates locally, and dispatch your applications through APIs. Treat the job hunt like an engineering system, and reclaim your clock cycles.
