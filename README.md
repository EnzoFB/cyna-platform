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

| Couche          | Technologie                                                                                 |
| --------------- | ------------------------------------------------------------------------------------------- |
| Backend         | Java 21, Spring Boot 3.4, Spring Security, Spring Data JPA, Gradle (Kotlin DSL)             |
| Base de données | PostgreSQL 16, Flyway (migrations versionnées)                                              |
| Auth            | JWT (access + refresh), OTP 2FA                                                             |
| Paiement        | Stripe (Checkout, SetupIntent, webhooks)                                                    |
| Frontend        | Angular 20, RxJS, `@ngx-translate`, `ngx-toastr`, AG Grid (backoffice), `@stripe/stripe-js` |
| Tests           | JUnit 5, ArchUnit (backend) — Karma + Jasmine (frontend)                                    |
| Doc API         | Swagger UI (`springdoc-openapi`)                                                            |

---

## Structure du dépôt

```
cyna-platform/
├── cyna-backend/         # API Spring Boot — monolithe modulaire
│   └── src/main/java/com/cyna/
│       ├── modules/{user,product,cart,order,subscription,payment}
│       └── shared/       # Kernel : Result, Mediator, AggregateRoot, …
├── cyna-frontend/        # Workspace Angular (3 projets)
│   └── projects/
│       ├── pwa/          # App client (port 4200)
│       ├── backoffice/   # App admin (port 4201)
│       └── ui-kit/       # Librairie de composants partagés
├── docs/                 # Documentation technique (architecture, modules, sécurité, …)
├── docker-compose.yml    # PostgreSQL 16 local
└── WORKFLOW_CYNA.md      # Conventions Git, issues, labels
```

Chaque module backend possède son propre schéma Postgres (`user_schema`, `product_schema`, …) et expose son contrat via `application/api/*`. Aucun accès direct aux tables d'un autre module — ces règles sont **vérifiées par ArchUnit** au build.

---

## Prérequis

- **Docker Desktop** (pour PostgreSQL local)
- **Java 21** (toolchain Gradle ; un JDK 21 doit être disponible)
- **Node.js LTS** + **npm**
- **Compte Stripe en mode test** (clés publishable + secret + webhook)
- _(optionnel)_ **Stripe CLI** pour relayer les webhooks en local

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

Flyway applique les migrations au démarrage. L'API écoute sur `http://localhost:8080/api/v1`.

### 4. Lancer le frontend (nouveau terminal)

```powershell
Set-Location cyna-frontend
npm install
npm run build:ui-kit     # construit la lib partagée (obligatoire au 1er run et à chaque modif)
npm run start:pwa        # http://localhost:4200
```

Backoffice (3e terminal, optionnel) :

```powershell
npm run start:backoffice # http://localhost:4201
```

### 5. _(optionnel)_ Relayer les webhooks Stripe

```powershell
stripe listen --forward-to localhost:8080/api/v1/payments/webhook
```

Coller le `whsec_…` affiché dans `STRIPE_WEBHOOK_SECRET`.

---

## Configuration (variables d'environnement)

Le backend lit automatiquement `cyna-backend/.env` au démarrage (`bootRun` exporte chaque ligne `KEY=VALUE`). Variables clés :

| Variable                                                      | Rôle                                                |
| ------------------------------------------------------------- | --------------------------------------------------- |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Connexion PostgreSQL                                |
| `JWT_SECRET`                                                  | Clé HS256 (≥ 256 bits) — surcharge le secret de dev |
| `JWT_ACCESS_EXPIRATION_HOURS`, `JWT_REFRESH_EXPIRATION_HOURS` | Durées de vie des tokens                            |
| `OTP_LOGIN_CODE_LENGTH`, `OTP_LOGIN_EXPIRATION_MINUTES`       | Paramètres du 2FA                                   |
| `STRIPE_SECRET_KEY`                                           | Clé secrète Stripe (`sk_test_…`) — **requise**      |
| `STRIPE_PUBLISHABLE_KEY`                                      | Clé publique Stripe (`pk_test_…`) — **requise**     |
| `STRIPE_WEBHOOK_SECRET`                                       | Secret webhook (`whsec_…`) — **requise**            |
| `SPRING_PROFILES_ACTIVE`                                      | `local`, `dev`, `prod`…                             |
| `SERVER_PORT`                                                 | Port HTTP du backend (défaut `8080`)                |

Voir [cyna-backend/.env.example](cyna-backend/.env.example) pour le template complet.

---

## URLs utiles

| Service         | URL                                   |
| --------------- | ------------------------------------- |
| PWA client      | http://localhost:4200                 |
| Backoffice      | http://localhost:4201                 |
| API REST        | http://localhost:8080/api/v1          |
| Swagger UI      | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON    | http://localhost:8080/v3/api-docs     |
| Actuator health | http://localhost:8080/actuator/health |

---

## Commandes courantes

### Backend (`cyna-backend/`)

```bash
./gradlew bootRun                      # lance l'API (charge .env automatiquement)
./gradlew build                        # build complet + tests
./gradlew build -x test                # build sans tests (CI)
./gradlew test                         # JUnit + ArchUnit
./gradlew test --tests '*OrderTest'    # une classe de test
./gradlew flywayMigrate                # applique les migrations (dev local uniquement)
./gradlew flywayInfo                   # état des migrations
./gradlew flywayClean                  # ⚠️ détruit les schémas — dev local uniquement
```

### Frontend (`cyna-frontend/`)

```bash
npm install
npm run start:pwa                      # ng serve pwa (4200)
npm run start:backoffice               # ng serve backoffice (4201)
npm run build:ui-kit                   # à rebuild après toute modif de ui-kit
npm run build:pwa
npm run build:backoffice
ng test pwa                            # tests Karma d'un projet
npm run lint
```

---

## Tests

- **Backend** : `./gradlew test` exécute les tests unitaires (JUnit 5) **et** les tests d'architecture (ArchUnit) qui garantissent le respect des frontières entre couches et modules. Un viol d'archi casse le build.
- **Frontend** : `ng test <project>` (Karma + Jasmine), par projet (`pwa`, `backoffice`, `ui-kit`).
- **CI** ([.github/workflows/ci.yml](.github/workflows/ci.yml)) sur chaque PR vers `develop`/`master`, 4 jobs :
    1. **`check-migrations`** — validation statique des fichiers Flyway (collisions de version `V{n}`, naming).
    2. **`build-backend`** — `./gradlew build -x test`.
    3. **`boot-backend`** — boote Spring Boot contre un Postgres 16 frais et hit `/actuator/health` → attrape les régressions Flyway, `ddl-auto: validate`, FK cassées.
    4. **`build-frontend`** — `npm ci` puis `build:ui-kit` → `build:pwa` → `build:backoffice`.

    **Les tests unitaires ne tournent pas en CI — à valider localement avant de pousser.**

---

## Documentation

La documentation technique complète vit dans [docs/](docs/). Points d'entrée recommandés :

- [Architecture Overview](docs/architecture/architecture-overview.md) — vision système, principes
- [Backend Architecture](docs/architecture/backend-architecture.md) — monolithe modulaire, Clean Architecture
- [Module Structure](docs/architecture/module-structure.md) — structure interne d'un module
- [Dependency Rules](docs/architecture/dependency-rules.md) — règles inter-couches enforced par ArchUnit
- [Inter-Module Communication](docs/architecture/inter-module-communication.md) — `application.api.*` + événements de domaine
- [Order & Payment Flow](docs/flows/order-payment-flow.md) — parcours d'achat complet (achat, renouvellement, 3DS, past_due)
- [Coding Standards](docs/development/coding-standards.md) — conventions de nommage par couche
- [Security Baseline](docs/security/security-baseline.md) — checklist sécurité

Index complet : [docs/README.md](docs/README.md).

---

## Workflow Git

- Branches : `master` (stable) ← `develop` (intégration) ← `feature/{ISSUE-ID}-{slug}`
- Les PR ciblent toujours `develop` (ou `master` pour les releases)
- Conventions complètes : [WORKFLOW_CYNA.md](WORKFLOW_CYNA.md) et [docs/workflow/git-workflow.md](docs/workflow/git-workflow.md)
- Revue de code : [docs/workflow/code-review-guidelines.md](docs/workflow/code-review-guidelines.md)

---

## Arrêt des services

```powershell
docker compose down            # arrête PostgreSQL (conserve les données)
docker compose down -v         # arrête PostgreSQL et supprime le volume (reset complet)
```

Backend et frontend s'arrêtent avec `Ctrl+C` dans leur terminal respectif.
