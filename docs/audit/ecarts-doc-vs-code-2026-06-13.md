# Audit — Écarts Documentation vs Implémentation

> Date : 2026-06-13 · Branche : `develop`
> Méthode : comparaison de la documentation (`docs/`, `CLAUDE.md`, source de vérité) avec le code réel (`cyna-backend/`, `cyna-frontend/`).
> Périmètre exclu : `.claude/worktrees/`, `.agents/`.

Légende sévérité : 🔴 CRITIQUE · 🟠 MAJEUR · 🟡 MINEUR

---

## Synthèse — écarts CRITIQUES

| # | Écart | Réf. doc | Preuve code |
|---|-------|----------|-------------|
| C1 | Module `notification` entièrement documenté mais **inexistant** | `notification-module.md`, `bounded-contexts.md:189-208` | aucun `modules/notification/` ; e-mail éparpillé dans `shared/` + `user/infrastructure/notification/` |
| C2 | Modules `order`/`payment`/`subscription` : doc décrit un design abandonné (refunds, state machine), réel = Stripe Subscriptions + webhooks | `order/payment/subscription-module.md`, `integration_stripe.md` | seul `order-payment-flow.md` est fiable |
| C3 | `domain` importe `application` (inversion Clean Architecture) | `dependency-rules.md:29,251` | `OrderRepository.java:3`, `ProductRepository.java:4`, `SubscriptionRepository.java:3` |
| C4 | Imports cross-module interdits (`BillingCycle`, `UserRepository`, `SubscriptionReadModel`) hors `application.api` | `dependency-rules.md:155-159` | `OrderLine.java:3`, `FinalizePaymentCommandHandler.java:15-16`, `ProcessSubscriptionAutoRenewRemindersCommandHandler.java:5` |
| C5 | `ui-kit` = coquille vide jamais consommée ; design system dupliqué dans `pwa/src/styles` | `frontend-architecture.md:48-55,153-169` | `projects/ui-kit/src/public-api.ts:9-26` |
| C6 | La CI **exécute** tous les tests alors que la doc affirme « tests not run in CI » | `testing-strategy.md:389`, `CLAUDE.md` | `ci.yml:92,297-301,446` |

---

## 1. Architecture backend

| # | Sév | Écart | Réf. doc | Preuve code |
|---|-----|-------|----------|-------------|
| A1 | 🔴 | `domain` → `application` | dependency-rules.md:29,251 | OrderRepository.java:3, ProductRepository.java:4, SubscriptionRepository.java:3 |
| A2 | 🔴 | Cross-module hors `application.api` (BillingCycle partagé de facto) | dependency-rules.md:155-159 ; inter-module L290-300 | OrderLine.java:3, FinalizePaymentCommandHandler.java:15-16, ProcessSubscriptionAutoRenewRemindersCommandHandler.java:5 (8+ fichiers) |
| A3 | 🟠 | Package réel `com.cyna.modules.{module}` ≠ doc `com.cyna.{module}` (tous les exemples + règles ArchUnit doc inexacts) | backend-architecture.md, module-structure.md, dependency-rules.md | tout `modules/*/` |
| A4 | 🟠 | Module `notification` documenté inexistant | bounded-contexts.md:189-208 | éclaté dans shared/ + user/infrastructure/notification |
| A5 | 🟠 | `interfaces` importe son `domain` (17 fichiers) | dependency-rules.md:103 | CreateProductRequest.java:3, PaymentConsentController.java:4, AuthController.java:14 |
| A6 | 🟠 | `interfaces` importe son `infrastructure` | dependency-rules.md:104,286 | AuthController.java:15-16 |
| A7 | 🟠 | 4-5 règles ArchUnit promises mais absentes du code | dependency-rules.md:244,251,265,286 | LayerDependencyRulesTest.java (6 règles) |
| A8 | 🟠 | `ModuleIsolationRulesTest` couvre 5 paires, manque subscription/cart/dashboard | module-structure.md:222 | ModuleIsolationRulesTest.java |
| A9 | 🟡 | Module `dashboard` non documenté | bounded-contexts.md (absent) | modules/dashboard/, dashboard_schema |
| A10 | 🟡 | Shared kernel : couche `interfaces` non documentée | backend-architecture.md:16-33,312-327 | shared/interfaces/rest |

## 2. Patterns backend

| # | Sév | Écart | Réf. doc | Preuve code |
|---|-----|-------|----------|-------------|
| P1 | 🟠 | `@Transactional` dans un command handler | transaction-management.md | payment/.../consentlog/LogPaymentConsentCommandHandler.java:8,21 |
| P2 | 🟠 | Listeners dans `infrastructure/event/` sans split `interfaces/eventlistener`+`application/eventhandler` | domain-events.md:239-254 ; CLAUDE.md | `*/infrastructure/event/*Listener.java` (8 fichiers) |
| P3 | 🟠 | E-mail de bienvenue via `@EventListener` synchrone (devrait être AFTER_COMMIT) | domain-events.md:337,349 | user/.../event/UserRegisteredListener.java:17-24 |
| P4 | 🟡 | Query handler `throw` sur échec métier | mediator-pattern.md:311 | cart/.../getcart/GetCartQueryHandler.java:26-28 |
| P5 | 🟡 | Exceptions pour forcer rollback au lieu de Result.failure | result-pattern.md:237-249 | payment/.../finalize/FinalizePaymentCommandHandler.java:219,236 |
| P6 | 🟡 | `interfaces` couplé aux constantes d'un handler `application` | mediator-pattern.md:307,322 | user/.../rest/AuthController.java:4,333,337 |

## 3. Base de données

| # | Sév | Écart | Réf. doc | Preuve code |
|---|-----|-------|----------|-------------|
| D1 | 🔴 | Règle réelle « script unique éditable + reset DB » non documentée et contredite par « append-only / immutable » → édition de V1 corrompt les checksums hors dev | dev/database-migrations.md:9 ; db/migrations.md:277 | db/migration/V1__create_schema.sql (seul script DDL) |
| D2 | 🟠 | `clean-disabled`/`validate-on-migrate`/`baseline-on-migrate` absents de la config Flyway | db/migrations.md:220-233 | application.yml:28-38 ; application-prod.yml |
| D3 | 🟠 | `dashboard_schema` réel non documenté ; `notification_schema` documenté inexistant | db/database-guidelines.md:15-21 | V1:9-15 ; application.yml:31-38 |
| D4 | 🟠 | FK cross-schema interdites par guidelines mais nombreuses (2 docs se contredisent) | db/database-guidelines.md:224-236 vs dev/database-migrations.md:25 | V1:537-539,578,588,609-610,645 |
| D5 | 🟠 | Seed de données métier via migration (V2) + script manuel non documentés | db/migrations.md:74 | V2__seed_data.sql ; scripts/seed-dashboard-2026.sql |
| D6 | 🟡 | Enum `users.status` doc (PENDING) ≠ réel (ANONYMIZED) | db/migrations.md:167 | V1:43,50 |
| D7 | 🟡 | Tables non pluralisées (`*_consent_log`, `dashboard_goal_settings`) | db/database-guidelines.md:40 | V1:181,643,666 |
| D8 | 🟡 | `updated_at`/`created_at` manquants sur ~13 tables | db/database-guidelines.md:198-203 | V1:61-90,225-232,632-636 |

## 4. Frontend

| # | Sév | Écart | Réf. doc | Preuve code |
|---|-----|-------|----------|-------------|
| F1 | 🔴 | `ui-kit` placeholder vide, jamais consommé | frontend-architecture.md:48-55,153-169 | projects/ui-kit/src/public-api.ts:9-26 ; 0 import `@cyna/ui-kit` |
| F2 | 🟠 | Interceptors Refresh/Error/Loading absents ; correlationId & errorLogging non documentés | frontend-architecture.md:92-96,259-265 | app.config.ts:35 ; core/interceptors/ |
| F3 | 🟠 | `GuestGuard` documenté inexistant | frontend-architecture.md:229 | core/guards/ |
| F4 | 🟠 | Tests : Jest/Cypress documentés, Karma/Jasmine réel | frontend-architecture.md:291 | package.json:49-57 |
| F5 | 🟠 | NgRx Signal Store + dossiers `store/` documentés, absents | frontend-architecture.md:119,174-183 | grep ngrx = 0 |
| F6 | 🟠 | Gabarit feature (components/services/models/store/index.ts) non respecté | frontend-architecture.md:108-129,151,315 | features/checkout à plat |
| F7 | 🟡 | Features PWA/backoffice en plus non documentées ; `settings` absent | frontend-architecture.md:195-221 | features/ |
| F8 | 🟡 | OnPush non systématique (~10/30 composants) | frontend-architecture.md:312 | grep OnPush=20 |

## 5. Sécurité

| # | Sév | Écart | Réf. doc | Preuve code |
|---|-----|-------|----------|-------------|
| S1 | 🟠 | Access token 1 h au lieu de 15 min | authentication.md:13,436 | application.yml:88 ; JwtProviderImpl.java:46 |
| S2 | 🟠 | Refresh token 24 h au lieu de 7 j | authentication.md:14,438 | application.yml:89 |
| S3 | 🟠 | Pas de lockout / rate-limit sur login échoué | authentication.md:444-445,459 | absent ; RateLimitingFilter.java:90 |
| S4 | 🟠 | Rôle SUPPORT documenté mais jamais appliqué | authorization.md:28,70-73 | SecurityConfig.java:144 ; Role.java:6 |
| S5 | 🟠 | Pas de validation longueur ≥256 bits du secret JWT | authentication.md:230,441 | JwtProviderImpl.java:31 |
| S6 | 🟡 | CORS allowedHeaders("*") + allowCredentials | https-headers.md / baseline | SecurityConfig.java:62-64 |
| S7 | 🟡 | Email (PII) dans le payload JWT | authentication.md:442 | JwtProviderImpl.java:41 |
| S8 | 🟡 | Réflexion de l'id dans messages d'erreur | xss.md:49-57 | OrderController.java:72,118 |
| S9 | 🟡 | Join cross-schema (order↔user) | CLAUDE.md | SpringDataOrderRepository.java:32-34 |
| S10 | 🟡 | `/account` accessible à tout authentifié, pas CUSTOMER-only | authorization.md:58-59 | SecurityConfig.java:142-145 |

## 6. Complétude fonctionnelle (modules)

| Module / Doc | Statut | Écarts majeurs |
|---|---|---|
| notification-module.md | ❌ NON IMPLÉMENTÉ | Module entier absent (🔴) |
| order-module.md | ⚠️ FORTEMENT DIVERGENT | États CONFIRMED/FULFILLED morts ; commands submit/confirm/fulfill absents (🔴) |
| payment-module.md | ⚠️ FORTEMENT DIVERGENT | Refund 100% absent ; API initiate/finalize/portal ≠ doc ; pas d'admin payment (🔴) |
| subscription-module.md | ⚠️ FORTEMENT DIVERGENT | Statuts ≠ ; activation par paiement (pas OrderFulfilled) ; admin subs absent (🔴) |
| product-module.md | ⚠️ DIVERGENT | `/admin/products` absent ; modèle translations/dualprice/categoryId ≠ (🔴) |
| user-module.md | ⚠️ DIVERGENT | OTP/account/addresses non doc ; admin deactivate/get-by-id ≠ (🟠) |
| cart-module.md | ✅ CONFORME (quasi) | `/cart/merge` non doc, guest cart non serveur (🟡) |
| dashboard (code) | 📄 NON DOCUMENTÉ | Module entier sans doc (🟡) |
| 2fa-otp-handover.md | ✅ EN AVANCE | « reste à faire » déjà fait (🟡) |
| integration_stripe.md | ⚠️ OBSOLÈTE | Décrit PaymentIntent simple, remplacé par Subscriptions (🟠) |
| order-payment-flow.md | ✅ FIABLE | Référence à jour |

## 7. Qualité / tests / API / Git

| # | Sév | Écart | Réf. doc | Preuve |
|---|-----|-------|----------|--------|
| Q1 | 🔴 | Docs disent « tests pas en CI », CI les exécute (backend+front+E2E) | testing-strategy.md:389 ; CLAUDE.md | ci.yml:92,297-301,446 |
| Q2 | 🟠 | Tâches Gradle `integrationTest`/`archTest` inexistantes | testing-strategy.md:380 ; architecture-tests.md:312 | build.gradle.kts:100-105 |
| Q3 | 🟠 | 3 des 5 classes ArchUnit absentes (naming/annotation/structural) | architecture-tests.md:178-304 | 2 fichiers dans architecture/ |
| Q4 | 🟠 | Git : trunk réel `master`/`develop`, pas `main` trunk-based | git-workflow.md:13-32 | branches develop/master |
| Q5 | 🟠 | Branches sans ticket ID, `feat/` vs `feature/` | git-workflow.md:35-56 | feat/logging-system, fix/full-prices |
| Q6 | 🟡 | Erreurs métier en 400 au lieu de 422 (mélange) | error-handling.md:284 | 9 ctrl badRequest vs 8 ctrl 422 |
| Q7 | 🟡 | ~34 tests hors format `should_..._when_` ; 9 usages de `any` | coding-standards.md:34,142 | divers |
| Q8 | 🟡 | Merge commits au lieu de squash ; commits non-conventionnels | git-workflow.md:62,230 | « Merge pull request #279 », « tests spec.ts » |

---

## Recommandations prioritaires

1. **Synchroniser la doc paiement** : réécrire `order/payment/subscription-module.md` + `integration_stripe.md` sur le modèle réel (`order-payment-flow.md`) ; marquer `notification-module.md` non implémenté ; documenter `dashboard`.
2. **Compléter les tests ArchUnit** pour attraper A1/A2, puis corriger ces violations (sortir `BillingCycle` vers `shared`).
3. **Trancher la gouvernance migrations** (D1) + forcer `clean-disabled: true` en prod (D2).
4. **Corriger la config sécurité** : durées de tokens (S1/S2), lockout login (S3), rôle SUPPORT (S4).
5. **Aligner la doc méta** : CI exécute les tests (Q1), GitFlow `master/develop` réel (Q4).
