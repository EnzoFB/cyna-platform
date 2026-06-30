# Cyna Platform

Plateforme SaaS de vente de services de cybersécurité (SOC, EDR, XDR) en ligne. Le système est conçu comme un **monolithe modulaire** suivant les principes de **Clean Architecture** et de **Domain-Driven Design**.

Le dépôt regroupe trois livrables : une **PWA client**, un **backoffice admin** et une **API REST** unique, le tout adossé à PostgreSQL et Stripe.

---

## Sommaire

- [Stack technique](#stack-technique)
- [Structure du dépôt](#structure-du-dépôt)
- [Prérequis](#prérequis)
- [Démarrage rapide](#démarrage-rapide)
- [Configuration (variables d'environnement)](#configuration-variables-denvironnement)
- [URLs utiles](#urls-utiles)
- [Commandes courantes](#commandes-courantes)
- [Tests](#tests)
- [Documentation](#documentation)
- [Workflow Git](#workflow-git)
- [Arrêt des services](#arrêt-des-services)

---

## Stack technique

| Couche | Technologie |
| --- | --- |
| Backend | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, Gradle (Kotlin DSL) |
| Base de données | PostgreSQL 16, Flyway (migrations versionnées) |
| Auth | JWT (access + refresh), OTP 2FA |
| Paiement | Stripe (Checkout, SetupIntent, webhooks) |
| Frontend | Angular 20, RxJS, `@ngx-translate`, `ngx-toastr`, AG Grid (backoffice), `@stripe/stripe-js` |
| Tests | JUnit 5, ArchUnit (backend), Karma + Jasmine (frontend), Playwright (E2E) |
| Doc API | Swagger UI (`springdoc-openapi`) |

---

## Structure du dépôt

```text
cyna-platform/
├── cyna-backend/         # API Spring Boot — monolithe modulaire
│   └── src/main/java/com/cyna/
│       ├── modules/{user,account,product,cart,order,subscription,payment,notification,dashboard}
│       └── shared/       # Kernel : Result, Mediator, AggregateRoot, …
├── cyna-frontend/        # Workspace Angular (3 projets)
│   └── projects/
│       ├── pwa/          # App client (port 4200)
│       ├── backoffice/   # App admin (port 4201)
│       └── ui-kit/       # Librairie de composants partagés
├── e2e/                  # Suite Playwright
├── docs/                 # Documentation technique
├── docker-compose.yml    # PostgreSQL 16 local
└── WORKFLOW_CYNA.md      # Conventions Git, issues, labels
```

Chaque module backend possède son propre schéma Postgres (`user_schema`, `product_schema`, …) et expose son contrat via `application/api/*`. Aucun accès direct aux tables d'un autre module : ces règles sont **vérifiées par ArchUnit** au build.

---

## Prérequis

- **Docker Desktop** (pour PostgreSQL local)
- **Java 21** (toolchain Gradle ; un JDK 21 doit être disponible)
- **Node.js LTS** + **npm**
- **Compte Stripe en mode test** (clés publishable + secret + webhook)
- *(optionnel)* **Stripe CLI** pour relayer les webhooks en local

---

## Démarrage rapide

Depuis la racine du projet :

### 1. Lancer PostgreSQL

```powershell
docker compose up -d
```

PostgreSQL écoute sur `localhost:5432` (base `cyna`, user `cyna`, password `cyna_dev_password`).

### 2. Configurer l'environnement backend

```powershell
Copy-Item cyna-backend\.env.example cyna-backend\.env
```

Renseigner les clés Stripe dans `cyna-backend/.env` (voir [Configuration](#configuration-variables-denvironnement)). L'application **refuse de démarrer** sans ces clés.

### 3. Lancer le backend

```powershell
Set-Location cyna-backend
.\gradlew.bat bootRun
```

Si nécessaire sur Windows :

```powershell
$env:JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\gradlew.bat bootRun
```

Flyway applique les migrations au démarrage. L'API écoute sur `http://localhost:8080/api/v1`.

### 4. Lancer le frontend

```powershell
Set-Location cyna-frontend
npm install
npm run build:ui-kit
npm run start:pwa
```

Backoffice (terminal séparé) :

```powershell
npm run start:backoffice
```

### 5. *(optionnel)* Relayer les webhooks Stripe

```powershell
stripe listen --forward-to localhost:8080/api/v1/payments/webhook
```

Coller le `whsec_...` affiché dans `STRIPE_WEBHOOK_SECRET`.

---

## Configuration (variables d'environnement)

Le backend lit automatiquement `cyna-backend/.env` au démarrage. Variables clés :

| Variable | Rôle |
| --- | --- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Connexion PostgreSQL |
| `JWT_SECRET` | Clé HS256 (≥ 256 bits) |
| `JWT_ACCESS_EXPIRATION_HOURS`, `JWT_REFRESH_EXPIRATION_HOURS` | Durées de vie des tokens |
| `OTP_LOGIN_CODE_LENGTH`, `OTP_LOGIN_EXPIRATION_MINUTES` | Paramètres du 2FA |
| `STRIPE_SECRET_KEY` | Clé secrète Stripe (`sk_test_...`) |
| `STRIPE_PUBLISHABLE_KEY` | Clé publique Stripe (`pk_test_...`) |
| `STRIPE_WEBHOOK_SECRET` | Secret webhook (`whsec_...`) |
| `SPRING_PROFILES_ACTIVE` | `local`, `dev`, `prod`, ... |
| `SERVER_PORT` | Port HTTP du backend (défaut `8080`) |

Voir `cyna-backend/.env.example` pour le template complet.

---

## URLs utiles

| Service | URL |
| --- | --- |
| PWA client | `http://localhost:4200` |
| Backoffice | `http://localhost:4201` |
| API REST | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Actuator health | `http://localhost:8080/actuator/health` |
| Grafana (logs) | `http://localhost:3000` |

---

## Déploiement en production

La plateforme se déploie via conteneurs : un backend Spring Boot (jar sur JRE),
un frontend nginx servant la PWA (`:80`) et le backoffice (`:81`) et relayant
`/api/v1` vers le backend, et PostgreSQL.

```bash
cp .env.prod.example .env.prod    # puis renseigner TOUTES les valeurs
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Le profil `prod` impose HTTPS + cookies `Secure` + HSTS et **échoue au démarrage**
si `JWT_SECRET`, `OTP_HASH_PEPPER` ou `CORS_ALLOWED_ORIGINS` manquent. Le build
frontend de prod applique `environment.prod.ts` (`apiUrl: /api/v1`) et injecte la
clé Stripe publishable *live* au build (`--build-arg STRIPE_PUBLISHABLE_KEY`).

📘 **Guide complet : [`docs/operations/deployment.md`](docs/operations/deployment.md)**
· ✅ **Checklist : [`docs/operations/production-readiness-checklist.md`](docs/operations/production-readiness-checklist.md)**

---

## Commandes courantes

### Backend (`cyna-backend/`)

```bash
./gradlew bootRun                      # lance l'API
./gradlew build                        # build complet + tests
./gradlew build -x test                # build sans tests
./gradlew test                         # JUnit + ArchUnit
./gradlew test --tests '*OrderTest'    # une classe de test
./gradlew flywayMigrate                # migrations (dev local uniquement)
./gradlew flywayInfo                   # état des migrations
./gradlew flywayClean                  # détruit les schémas (dev local uniquement)
```

### Frontend (`cyna-frontend/`)

```bash
npm install
npm run start:pwa
npm run start:backoffice
npm run build:ui-kit
npm run build:pwa
npm run build:backoffice
ng test pwa
npm run lint
```

### E2E (`e2e/`)

```bash
npm install
npm test
npm run test:core
npm run test:pwa
npm run test:pwa-mobile
npm run test:backoffice
```

---

## Tests

- **Backend** : `./gradlew test` exécute les tests unitaires (JUnit 5) et les tests d'architecture (ArchUnit).
- **Frontend** : `ng test <project>` exécute les tests Karma/Jasmine par projet (`pwa`, `backoffice`, `ui-kit`).
- **E2E** : la suite Playwright vit dans `e2e/`.
  - `npm run test:core` couvre la suite principale PR/CI : chargement applicatif, navigation, authentification, pages publiques, pages protégées, formulaires, compte client, permissions backoffice, CRUD admin, recherche, filtres, pagination et responsive PWA.
  - Les parcours Stripe complets restent dans les specs dédiées et demandent les prérequis locaux correspondants (`stripe listen`, clés de test, webhooks).
- **CI** : `.github/workflows/ci.yml` exécute sur chaque PR :
  1. `check-migrations`
  2. `backend-lint`
  3. `backend-build`
  4. `backend-boot`
  5. `frontend-lint`
  6. `frontend-build`
  7. `frontend-test`
  8. `e2e-core`

---

## Documentation

La documentation technique complète vit dans `docs/`. Points d'entrée recommandés :

- `docs/architecture/architecture-overview.md` — vision système
- `docs/architecture/backend-architecture.md` — monolithe modulaire, Clean Architecture
- `docs/architecture/module-structure.md` — structure interne d'un module
- `docs/architecture/dependency-rules.md` — règles inter-couches
- `docs/architecture/inter-module-communication.md` — `application.api.*` et événements de domaine
- `docs/flows/order-payment-flow.md` — parcours d'achat complet
- `docs/operations/logging-system.md` — monitoring centralisé (Grafana + Loki + Promtail)
- `docs/development/coding-standards.md` — conventions de nommage
- `docs/security/security-baseline.md` — baseline sécurité

Index complet : `docs/README.md`.

---

## Workflow Git

- Branches : `master` (stable) ← `develop` (intégration) ← `feature/{ISSUE-ID}-{slug}`
- Les PR ciblent toujours `develop` (ou `master` pour les releases)
- Conventions complètes : `WORKFLOW_CYNA.md` et `docs/workflow/git-workflow.md`
- Revue de code : `docs/workflow/code-review-guidelines.md`

---

## Arrêt des services

```powershell
docker compose down
docker compose down -v
```

Backend et frontend s'arrêtent avec `Ctrl+C` dans leur terminal respectif.
