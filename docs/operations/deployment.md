# Production Deployment Guide

This guide takes the Cyna platform from a clean checkout to a running
production deployment. It targets a single host (or VM) running Docker, with
TLS terminated by an upstream reverse proxy / load balancer. The same images
work unchanged on Kubernetes or any container platform.

> **Audience:** the operator deploying and delivering the platform.
> **Companion:** run through the [Production Readiness Checklist](production-readiness-checklist.md) before going live.

---

## 1. Architecture at deploy time

```
                         ┌─────────────────────── TLS / Ingress ───────────────────────┐
                         │  (Let's Encrypt, ALB, Cloudflare, nginx, …)                  │
                         │  sets X-Forwarded-Proto=https                                │
                         └───────┬───────────────────────────────────┬─────────────────┘
                                 │ app.cyna.com                       │ admin.cyna.com
                                 ▼                                     ▼
                       ┌───────────────────── frontend (nginx) ─────────────────────┐
                       │  :80  Customer PWA      :81  Backoffice                     │
                       │  serves SPA + proxies /api/v1/* ─────────────┐              │
                       └──────────────────────────────────────────────┼─────────────┘
                                                                       ▼
                                                          ┌──────── backend ────────┐
                                                          │  Spring Boot :8080       │
                                                          │  profile = prod          │
                                                          └────────────┬─────────────┘
                                                                       ▼
                                                          ┌──────── postgres ───────┐
                                                          │  16-alpine, volume       │
                                                          └──────────────────────────┘
```

- The browser only ever talks to the frontend origin; `/api/v1/*` is proxied
  to the backend, so **no CORS is needed for same-origin calls** — CORS only
  matters if you serve the PWA and the API from different hosts.
- The backend trusts `X-Forwarded-Proto` (prod profile, `forward-headers-strategy: framework`),
  which is what makes `Secure` cookies and HSTS work behind the proxy.

---

## 2. Prerequisites

| Requirement | Notes |
|---|---|
| Docker Engine + Compose v2 | `docker compose version` ≥ 2.x |
| A domain with 2 hostnames | e.g. `app.cyna.com` (PWA) + `admin.cyna.com` (backoffice) |
| TLS termination | reverse proxy / load balancer / ingress in front of the frontend container |
| Stripe **live** account | secret key, publishable key, a webhook endpoint, optional Tax registration |
| Brevo account | transactional email API key + verified sender |

---

## 3. Configure secrets

```bash
cp .env.prod.example .env.prod
```

Fill **every** value. The non-negotiable ones (the app refuses to start without
them, by design):

| Variable | How to generate / where to get it |
|---|---|
| `JWT_SECRET` | `openssl rand -base64 48` (≥256 bits) |
| `OTP_HASH_PEPPER` | `openssl rand -base64 32` (≥16 bytes) |
| `DB_PASSWORD` | `openssl rand -base64 24` |
| `CORS_ALLOWED_ORIGINS` | your exact front-end origins, comma-separated, **no trailing slash** |
| `STRIPE_SECRET_KEY` / `STRIPE_PUBLISHABLE_KEY` / `STRIPE_WEBHOOK_SECRET` | Stripe Dashboard (live mode) |
| `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`, `CONTACT_TO_EMAIL`, `APP_PUBLIC_URL` | your Brevo + domain config |

`.env.prod` is git-ignored. Rotating `JWT_SECRET` invalidates all access tokens
(users transparently re-`/auth/refresh`). Rotating `OTP_HASH_PEPPER` invalidates
in-flight OTP challenges only.

---

## 4. Build & run

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

This builds three images and starts the stack:

- **postgres** — internal only, data in the `pgdata-prod` volume. Healthchecked.
- **backend** — Spring Boot fat jar on a JRE, `SPRING_PROFILES_ACTIVE=prod`.
  **Flyway runs the migrations automatically at startup** (`ddl-auto: validate`,
  so the schema is checked against the entities). Healthchecked on `/actuator/health`.
- **frontend** — nginx serving the PWA on `:80` and the backoffice on `:81`,
  proxying `/api/v1` to the backend. The live Stripe publishable key is baked
  in at build time from `STRIPE_PUBLISHABLE_KEY`.

Point your TLS proxy at the frontend container: `app.<domain>` → `:80`,
`admin.<domain>` → `:81`.

### Verify

```bash
docker compose -f docker-compose.prod.yml ps          # all healthy
curl -fsS http://<host>/healthz                        # nginx PWA -> ok
curl -fsS http://<host>/api/v1/../actuator/health      # via backend container
docker compose -f docker-compose.prod.yml logs backend | tail
```

---

## 5. Stripe webhook (required for orders/subscriptions)

In the Stripe Dashboard (**live mode**) → Developers → Webhooks → add endpoint:

- **URL:** `https://app.cyna.com/api/v1/payments/webhook`
- **Events:** the subscription/payment-intent/invoice events the payment module
  consumes (see [`docs/flows/order-payment-flow.md`](../flows/order-payment-flow.md)).
- Copy the signing secret into `STRIPE_WEBHOOK_SECRET` and redeploy the backend.

The webhook is signature-verified and idempotent (`ProcessedStripeEvent`), so
retries are safe.

---

## 6. Building images individually (CI / registry)

If you push to a registry instead of building on the host:

```bash
# Backend
docker build -t registry.example.com/cyna-backend:<tag> ./cyna-backend

# Frontend (bake the live publishable key)
docker build -t registry.example.com/cyna-frontend:<tag> \
  --build-arg STRIPE_PUBLISHABLE_KEY=pk_live_xxx ./cyna-frontend
```

Then reference the images (instead of `build:`) in your orchestrator.

---

## 7. Updates & rollback

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

- Migrations are forward-only (Flyway). Review new `V*.sql` before deploying.
- Roll back by redeploying the previous image tag; if a migration must be
  reverted, write a new compensating migration (never edit an applied one).
- Back up Postgres before every deploy: `docker exec cyna-postgres-prod pg_dump -U $DB_USERNAME $DB_NAME > backup.sql`.

---

## 8. Observability (optional)

`docker-compose.yml` (the dev stack) contains Loki + Grafana + Promtail. For
production, run a hardened equivalent (enable auth, set retention per the
[RGPD doc](../legal/rgpd-compliance.md) since logs carry `clientIp`). The
backend already emits structured JSON logs (logstash encoder) ready for
ingestion.

---

## 9. What the `prod` profile changes vs local

| Setting | local | prod |
|---|---|---|
| HTTPS enforced + Secure cookies | off | **on** (`require-https: true`) |
| HSTS | off | **on** |
| `X-Forwarded-*` honoured | no | **yes** |
| CORS origins | localhost default | **`CORS_ALLOWED_ORIGINS` (required)** |
| OTP pepper | dev default | **`OTP_HASH_PEPPER` (required)** |
| JWT secret | `JWT_SECRET` (required everywhere) | required |
| SQL/web debug logs | verbose | quieted (WARN/INFO) |

See [`docs/security/security-baseline.md`](../security/security-baseline.md) for
the full security posture.
