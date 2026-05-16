# RGPD / GDPR Compliance

Authoritative reference for how the CYNA platform meets the GDPR. Scope:
what we process, on what basis, for how long, how the data-subject rights are
served, and the operational procedures that live **outside** the code.

> This documents the technical and organizational measures implemented. It is
> not legal advice — a DPO / legal review is recommended before production.

---

## 1. Register of processing activities

| Processing | Purpose | Legal basis (Art. 6) | Data categories | Recipients / processors | Retention |
|---|---|---|---|---|---|
| Account management | Provide the service, authentication | 6.1.b contract | Identity, professional email, company, hashed password | — | Life of the account, then anonymized/deleted on erasure |
| Orders & subscriptions | Sell and operate subscriptions | 6.1.b contract | Order/subscription history, amounts | — | **10 years** (accounting) |
| Invoicing | Legal accounting obligation | 6.1.c legal obligation | Billing identity & address, amounts | **Stripe** (processor) | **10 years** (Code de commerce L123-22) — held by Stripe |
| Payment | Take payment | 6.1.b contract | Card token (`pm_…`), brand/last4/expiry | **Stripe** (processor, PCI-DSS) | Stripe vault; local cache purged on erasure |
| Card reuse consent | Reuse a saved card | 6.1.a consent | Stripe PM id, label version, IP, user-agent, timestamp | — | **5 years** (proof) |
| Terms/Privacy consent | Prove informed acceptance | 6.1.a / 7.1 | Action, label version, IP, user-agent, timestamp | — | **5 years** (proof) |
| Transactional email | Order/subscription notifications | 6.1.b contract | Email, name | **Brevo** (processor) | Not stored beyond send |
| Security & sessions | Auth, fraud/abuse prevention | 6.1.f legitimate interest | Refresh tokens (hashed), trusted-device tokens, OTP challenges | — | Short-lived; revoked on erasure |

**Processors** operate under a Data Processing Agreement: **Stripe** (payments,
invoicing — also acts as controller for its own legal invoice retention) and
**Brevo** (transactional email). Personal data is never sold.

PAN/CVV never reach our servers (Stripe Elements tokenization → **PCI-DSS
SAQ A**); see [payment-methods-operations.md](../setup/payment-methods-operations.md).

---

## 2. Retention durations

| Data | Duration | Source |
|---|---|---|
| Active account | Account lifetime | Contract |
| Orders / invoices | 10 years | Code de commerce L123-22 (accounting) |
| Tax-relevant records | 6 years | LPF Art. L102 B |
| Proof of consent | 5 years | Litigation prescription, Code civil Art. 2224 |
| Auth/session artifacts | Minutes–days | Security necessity |

End-of-retention purge is a **documented manual procedure** (see §5), not an
automated job — proportionate for the current scale.

---

## 3. Right to erasure — the decision model (Art. 17)

Erasure is **not** always "delete". The lawful outcome depends on whether the
account carries a legally-retained transactional footprint:

```
erasure request (self-service OR admin)
        │
        ▼
  user has any order?  ──no──►  HARD DELETE
        │                       (FK ON DELETE CASCADE removes addresses,
        │                        trusted devices, OTP/reset/email-change
        │                        tokens, payment_consent_log)
        │ yes
        ▼
  ANONYMIZE-AND-KEEP
   • email/name scrubbed, password → non-loginable sentinel, status=ANONYMIZED
   • sessions + trusted devices revoked (login refused for any non-ACTIVE)
   • addresses purged (billing address retained on Stripe invoices)
   • local saved_payment_methods purged (Stripe Customer kept for invoices)
   • orders/subscriptions kept (no PII — surrogate userId only)
   • payment_consent_log kept (Art. 7.1 proof)
```

- Implemented by `AccountErasure` (shared by `DeleteMyAccountCommand`
  self-service and `DeleteUserCommand` admin) — the trigger never changes the
  outcome.
- "Anonymize" = irreversible scrub of direct identifiers (not a reversible
  soft-delete flag). The row survives only as a non-identifying FK anchor for
  records the law forces us to keep.
- Where data must be kept but cannot be anonymized (legal invoice mentions),
  it is **restricted** (Art. 18) on the Stripe side and purged at end of the
  legal period (§5). Locally, our order/subscription rows hold no PII.

---

## 4. Data-subject rights — how they are served

| Right | How |
|---|---|
| Access (15) / Portability (20) | Self-service `GET /api/v1/account/export` → structured JSON ("My account → Data & Privacy → Download my data"). Invoices are not duplicated (held by Stripe, downloadable from billing history). |
| Erasure (17) | Self-service `DELETE /api/v1/account` + admin `DELETE /api/v1/admin/users/{id}` → decision model in §3 |
| Rectification (16) | Profile / address edit screens |
| Restriction (18) / Objection (21) | Manual via contact; for retained accounting data, restriction is the default end-state until purge |
| Information (13/14) | Privacy Policy page (`/privacy-policy`), linked in the footer; consent box at registration |
| Withdraw consent (7.3) | Card-reuse: managed via the Stripe Customer Portal / payment methods tab |
| Proof of consent (7.1) | `user_consent_log` (terms/privacy at registration) + `payment_consent_log` (card reuse) — append-only, with version/IP/UA/timestamp |

Complaints: data subjects may refer to the **CNIL** (www.cnil.fr).

---

## 5. Operational procedure — end-of-retention purge (manual)

Run periodically (e.g. yearly) by an authorized operator:

1. Identify accounts `status = ANONYMIZED` whose last order is older than the
   accounting retention (10 years) **and** outside any litigation window.
2. For each: confirm no open dispute, then delete the residual order/
   subscription rows and the surrogate user row.
3. On the Stripe side, request deletion of the corresponding Customer once the
   invoice-retention obligation has lapsed (Stripe is the controller for that
   retention).
4. Purge `payment_consent_log` / `user_consent_log` entries older than 5 years
   with no active processing they evidence.
5. Record the purge run (date, scope, operator) for accountability.

> Automating this as a scheduled job is a deliberate non-goal at this scale
> (avoids an unvalidated destructive job operating on dated data). Revisit if
> volume grows.

---

## 6. Cookies

The application sets **only strictly necessary cookies** (authentication,
refresh, trusted-device, session security). No analytics, advertising or
tracking cookies are used. Under the ePrivacy directive and CNIL guidelines,
strictly necessary cookies are exempt from prior consent — **no cookie banner
is legally required**. This is stated in the Privacy Policy.

---

## 7. Data minimization & security measures

- Passwords hashed; access tokens short-lived in memory; refresh tokens
  HttpOnly-cookie + hashed at rest, with rotation, reuse-detection and
  revocation.
- Card data tokenized at Stripe (SAQ A) — never stored locally beyond
  brand/last4/expiry display metadata.
- Per-module Postgres schemas; cross-module access only via published APIs /
  domain events (no cross-schema PII joins).
- Consent records are append-only and tamper-evident (server-stamped IP/UA,
  version pinned to the wording actually shown).

---

## 8. Residual limitations (honest assessment)

- End-of-retention purge is manual — relies on an operator running §5.
- Stripe holds card + invoice data under its own retention; our erasure cannot
  force immediate deletion there (legal retention + processor boundary).
- Free-text fields (e.g. address lines) are scrubbed by row deletion on
  anonymization, not field-level redaction — acceptable because the row has no
  standalone retention basis.
- `payment_consent_log` / `user_consent_log` keep IP + user-agent as consent
  evidence (Art. 7.1) for 5 years — necessary and proportionate, documented
  here for transparency.
