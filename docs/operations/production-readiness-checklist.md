# Production Readiness Checklist

Run through this before delivering / going live. See
[deployment.md](deployment.md) for the how-to.

## Secrets & configuration
- [ ] `.env.prod` created from `.env.prod.example`, **every** value filled
- [ ] `JWT_SECRET` is a fresh ≥256-bit random value (not the example)
- [ ] `OTP_HASH_PEPPER` is a fresh ≥16-byte random value (not the dev default)
- [ ] `DB_PASSWORD` is strong and unique
- [ ] `CORS_ALLOWED_ORIGINS` lists the exact prod front-end origins, no trailing slash
- [ ] Stripe **live** keys set (`sk_live_…`, `pk_live_…`, `whsec_…`)
- [ ] Brevo API key + verified sender + `CONTACT_TO_EMAIL` + `APP_PUBLIC_URL` set
- [ ] `.env.prod` is **not** committed (`git check-ignore .env.prod` → ignored)

## Build correctness (the bugs this release fixes)
- [ ] Frontend prod build uses `environment.prod.ts` — `apiUrl` is `/api/v1`,
      **not** `http://localhost:8080` (verify: `grep -r localhost:8080 cyna-frontend/dist` returns nothing)
- [ ] PWA bundle carries the **live** publishable key, not `pk_test_…` or the placeholder
      (verify: `grep -ro 'pk_[a-z]*_' cyna-frontend/dist/pwa` shows `pk_live_`)
- [ ] Backend starts with `SPRING_PROFILES_ACTIVE=prod` and **fails fast** if
      `JWT_SECRET`, `OTP_HASH_PEPPER`, or `CORS_ALLOWED_ORIGINS` is missing

## Infrastructure
- [ ] TLS terminated upstream; proxy sets `X-Forwarded-Proto=https`
- [ ] DNS: `app.<domain>` → frontend `:80`, `admin.<domain>` → frontend `:81`
- [ ] Postgres data on a persistent, backed-up volume
- [ ] Stripe live webhook endpoint created → `https://app.<domain>/api/v1/payments/webhook`,
      signing secret in `STRIPE_WEBHOOK_SECRET`
- [ ] DB backup taken before first deploy and before each subsequent deploy

## Validation after deploy
- [ ] `docker compose -f docker-compose.prod.yml ps` — all services healthy
- [ ] `/actuator/health` returns `{"status":"UP"}`
- [ ] Customer can register → log in (OTP email received) → browse catalog
- [ ] Checkout completes against Stripe live (use a real low-value test, then refund)
- [ ] A Stripe webhook event is received and processed (check backend logs)
- [ ] Admin can log in to the backoffice (admin login always requires OTP)
- [ ] Contact form submits successfully (email arrives at `CONTACT_TO_EMAIL`)
- [ ] Non-admin user is rejected from the backoffice and admin APIs (403)

## Recommended hardening (not blocking, tracked from the audit)
- [ ] Enable Dependabot + a dependency/container vuln scan (Trivy/OWASP) in CI
- [ ] Restrict `X-Forwarded-For` trust to the known proxy for rate-limiting
- [ ] Run a hardened observability stack (Loki auth on, PII retention per RGPD)
- [ ] Tighten CORS `allowedHeaders` from `*` to the explicit set used
