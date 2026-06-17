# syntax=docker/dockerfile:1

# ---------- Stage 1: build the Angular PWA + Backoffice ----------
FROM node:24-bookworm AS build
WORKDIR /app

# Install deps from the lockfile (reproducible).
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Inject the LIVE Stripe publishable key into the prod environment file.
# Publishable keys are public by design, but we keep the live value out of
# git and pass it at build time instead.
ARG STRIPE_PUBLISHABLE_KEY=pk_test_placeholder
RUN sed -i "s|__STRIPE_PUBLISHABLE_KEY__|${STRIPE_PUBLISHABLE_KEY}|g" \
      projects/pwa/src/environments/environment.prod.ts

# Production builds (defaultConfiguration=production → applies fileReplacements).
RUN npm run build:pwa && npm run build:backoffice

# ---------- Stage 2: nginx serving both SPAs ----------
FROM nginx:1.27-alpine AS runtime

# Angular 20 (@angular/build:application) emits to dist/<app>/browser.
COPY --from=build /app/dist/pwa/browser        /usr/share/nginx/html/pwa
COPY --from=build /app/dist/backoffice/browser /usr/share/nginx/html/backoffice
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# 80 = customer PWA, 81 = backoffice. Both proxy /api/v1 → backend:8080.
EXPOSE 80 81

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:80/healthz >/dev/null 2>&1 || exit 1
