# HTTPS and Security Headers

## Purpose

This document defines HTTPS enforcement and security headers for the CYNA Platform.
Goal: protect against downgrade attacks and common browser‑side threats without impacting performance.

---

## HTTPS

- In production, HTTPS must be enforced at the edge (reverse proxy / load balancer).
- The backend can enforce HTTPS at the application layer when `app.security.require-https=true`.
- If the app is behind a proxy, enable forwarded headers support in the runtime environment.

Recommended settings:

```
app.security.require-https=true
```

---

## Security Headers

The backend sets these headers:

- `Strict-Transport-Security` (HSTS)
- `Content-Security-Policy`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy`
- `Permissions-Policy`

Default values are configured in `application.yml` and can be overridden per environment.

---

## Configuration

```yaml
app:
  security:
    require-https: true
    hsts:
      enabled: true
      max-age-seconds: 31536000
      include-subdomains: true
      preload: true
    content-security-policy: "default-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'"
    referrer-policy: "no-referrer"
    permissions-policy: "geolocation=(), microphone=(), camera=()"
```

---

## Review Checklist

- HTTPS enforced in production (proxy or `require-https`)
- HSTS enabled with a long max‑age
- CSP is defined and reviewed for new UI needs
- No unsafe headers are introduced

---

## Related Documents

- [API Guidelines](../api/api-guidelines.md)
- [Authorization](authorization.md)
