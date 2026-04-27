# Security Baseline

## Purpose

This document consolidates the mandatory security rules for backend tickets.
Use it as the default checklist for new changes.

---

## 1. SQL Injection

- Never concatenate user input into SQL/JPQL
- Use parameter binding
- Sort fields must be allow-listed

Reference: [SQL Injection Protection](sql-injection.md)

---

## 2. XSS

- Reject HTML in user-facing text fields
- Use `@NoHtml` on request DTOs
- No `[innerHTML]` rendering on the frontend

Reference: [XSS Protection](xss.md)

---

## 3. HTTPS & Security Headers

- Enforce HTTPS in production
- Enable HSTS, CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy

Reference: [HTTPS & Security Headers](https-headers.md)

---

## 4. CSRF

- Enable CSRF when using cookie-based sessions
- Use CSRF token cookie + `X-CSRF-TOKEN` header
- Explicitly ignore public endpoints

Reference: [CSRF Protection](csrf.md)
