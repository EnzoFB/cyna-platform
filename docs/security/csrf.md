# CSRF Protection

## Purpose

This document defines CSRF (Cross‑Site Request Forgery) protection for the CYNA Platform.

---

## Rules

1. CSRF protection is required for browser sessions that use cookies.
2. For stateless JWT APIs, CSRF can be disabled by default and enabled per environment when needed.
3. When enabled, the backend uses a CSRF token cookie and expects `X-CSRF-TOKEN` on unsafe requests.
4. Authentication and public documentation endpoints can be excluded from CSRF checks.

---

## Configuration

```yaml
app:
  security:
    csrf:
      enabled: true
      ignored-paths:
        - /api/v1/auth/**
        - /swagger-ui/**
        - /v3/api-docs/**
        - /actuator/**
```

---

## Review Checklist

- CSRF enabled when using cookie-based auth
- Frontend sends `X-CSRF-TOKEN` for POST/PUT/PATCH/DELETE
- Public endpoints are explicitly excluded

---

## Related Documents

- [API Guidelines](../api/api-guidelines.md)
