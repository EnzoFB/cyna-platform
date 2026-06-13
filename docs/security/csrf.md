# CSRF Protection

## Purpose

This document defines CSRF (Cross‑Site Request Forgery) protection for the CYNA Platform.

---

## Rules

1. CSRF protection is required for browser sessions that use cookies.
2. For stateless JWT APIs, CSRF can stay scoped to the cookie-authenticated routes only.
3. The backend uses a CSRF token cookie and expects `X-XSRF-TOKEN` on protected unsafe requests.
4. In CYNA, CSRF is enforced on `POST /api/v1/auth/refresh` and `POST /api/v1/auth/logout`.
5. SPAs bootstrap the token through `GET /api/v1/auth/csrf`.

---

## Configuration

```yaml
app:
  security:
    csrf:
      enabled: true
      ignored-paths:
        - /swagger-ui/**
        - /v3/api-docs/**
        - /actuator/**
```

---

## Review Checklist

- CSRF enabled when using cookie-based auth
- Frontend sends `X-XSRF-TOKEN` for protected routes
- `/auth/refresh` and `/auth/logout` reject requests without a valid token
- Public docs/actuator endpoints are explicitly excluded

---

## Related Documents

- [API Guidelines](../api/api-guidelines.md)
