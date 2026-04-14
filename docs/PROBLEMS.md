# Problems to Fix

This file lists all inconsistencies found between documentation and code, as well as internal inconsistencies across documentation files. These items need to be resolved to align the codebase with the documented design.

---

## Legend

- **[CODE]** — Code does not match documentation. Code needs to be updated.
- **[DOC-CONFLICT]** — Inconsistency between two documentation files.
- **[MISSING-CODE]** — Feature documented but not yet implemented.
- **[MISSING-DOC]** — Feature exists in code but not documented anywhere.

---

## 1. Product Module

### 1.1 Domain Model

| # | Type | Problem | Documentation Says | Code Does |
|---|------|---------|-------------------|--------------------|
| 1 | [CODE] | Product uses raw `UUID` instead of `ProductId` value object | `ProductId` (non-null UUID wrapper) | Raw `UUID` |
| 2 | [CODE] | Product uses raw `String` instead of `ProductName` value object | `ProductName` (non-blank, max 255 chars) | Raw `String` |
| 3 | [CODE] | Single `description` field missing, replaced by two fields | `description: String` | `serviceDescription` + `technicalDescription` |
| 4 | [CODE] | `features` field does not exist | `features: List<Feature>` with name + description | No `features` field or `Feature` value object |
| 5 | [CODE] | Category enum values differ | `Category` enum: `ENDPOINT`, `NETWORK`, `CLOUD` | `ProductCategory` enum: `SOC`, `EDR`, `XDR` |
| 6 | [CODE] | Pricing model differs — single price vs dual price | `price: Money` + `billingCycle: BillingCycle` | `monthlyPrice: Money` + `annualPrice: Money` (no billingCycle on Product) |
| 7 | [CODE] | Publication status enum differs | `Availability`: `PUBLISHED`, `UNPUBLISHED` (default: `UNPUBLISHED`) | `ProductStatus`: `DRAFT`, `PUBLISHED`, `UNPUBLISHED` (default: `DRAFT`) |
| 8 | [CODE] | `BillingCycle` QUARTERLY not implemented | `BillingCycle`: `MONTHLY`, `QUARTERLY`, `ANNUAL` | `BillingCycle` (in Cart): `MONTHLY`, `ANNUAL` only |
| 9 | [MISSING-CODE] | `ProductPriority` not documented but exists in code | Not in docs | `ProductPriority` enum: `NORMALE`, `MOYENNE`, `HAUTE` |

### 1.2 Domain Behaviors

| # | Type | Problem |
|---|------|---------|
| 10 | [MISSING-CODE] | `Product.publish()` method does not exist in code |
| 11 | [MISSING-CODE] | `Product.unpublish()` method does not exist in code |
| 12 | [MISSING-CODE] | `Product.changePrice(newPrice)` method does not exist in code |
| 13 | [MISSING-CODE] | `Product.update(...)` method does not exist in code (handler uses `reconstitute()`) |

### 1.3 Domain Events

| # | Type | Problem |
|---|------|---------|
| 14 | [MISSING-CODE] | `ProductCreated` event does not exist in code |
| 15 | [MISSING-CODE] | `ProductUpdated` event does not exist in code |
| 16 | [MISSING-CODE] | `ProductPublished` event does not exist in code |
| 17 | [MISSING-CODE] | `ProductUnpublished` event does not exist in code |
| 18 | [MISSING-CODE] | `PriceChanged` event does not exist in code |
| 19 | [MISSING-DOC] | `CategoryCreated` event exists in code but is absent from the product-module doc |
| 20 | [MISSING-DOC] | `CategoryUpdated` event exists in code but is absent from the product-module doc |
| 21 | [MISSING-DOC] | `CategoryDeleted` event exists in code but is absent from the product-module doc |

### 1.4 Commands & Queries

| # | Type | Problem |
|---|------|---------|
| 22 | [CODE] | `CreateProductCommand` fields don't match (doc: name, description, category, price, currency, billingCycle, features / code: name, category, priority, serviceDescription, technicalDescription, monthlyPrice, annualPrice, currency) |
| 23 | [CODE] | `UpdateProductCommand` fields don't match (same differences as Create) |
| 24 | [MISSING-CODE] | `PublishProductCommand` / `PublishProductCommandHandler` does not exist |
| 25 | [MISSING-CODE] | `UnpublishProductCommand` / `UnpublishProductCommandHandler` does not exist |
| 26 | [MISSING-CODE] | `ChangePriceCommand` / `ChangePriceCommandHandler` does not exist |
| 27 | [MISSING-CODE] | `ListPublishedProductsQuery` does not exist (code uses `ListProductsQuery` with a `status` filter) |
| 28 | [CODE] | `ListProductsQuery` is much richer in code (page, size, status, category, search, sort) vs doc (category, availability only) |
| 29 | [CODE] | `ProductReadModel` fields don't match doc (code has dual prices, priority, two descriptions, status) |

### 1.5 Repository

| # | Type | Problem |
|---|------|---------|
| 30 | [CODE] | `ProductRepository.findAll()` in code is paginated/filtered vs doc's simple `findAll()` |
| 31 | [CODE] | `ProductRepository.findAllPublished()` does not exist in code |
| 32 | [CODE] | Repository methods use `UUID` not `ProductId` |

### 1.6 REST Endpoints

| # | Type | Problem |
|---|------|---------|
| 33 | [CODE] | Admin product routes are at `/api/v1/products` (no `/admin/` prefix in code) — doc says `/api/v1/admin/products` |
| 34 | [CODE] | No separate admin GET endpoint — code's `GET /api/v1/products` is `permitAll()` and serves both public and admin |
| 35 | [MISSING-CODE] | `POST /api/v1/admin/products/{id}/publish` does not exist |
| 36 | [MISSING-CODE] | `POST /api/v1/admin/products/{id}/unpublish` does not exist |
| 37 | [MISSING-CODE] | `PUT /api/v1/admin/products/{id}/price` does not exist |
| 38 | [CODE] | `ProductResponse` in code uses nested `PriceResponse` (amount + currency) vs doc's flat `price` + `currency` |

### 1.7 Category (fully undocumented in current product-module.md)

| # | Type | Problem |
|---|------|---------|
| 39 | [MISSING-DOC] | `Category` aggregate exists in code (name, description, image, active, timestamps) but is only mentioned as an enum in the doc |
| 40 | [MISSING-DOC] | `CategoryRepository` interface exists in code (save, findById, findByName, existsByName, findAll, deleteById) |
| 41 | [MISSING-DOC] | `CreateCategoryCommand` + handler exists in code |
| 42 | [MISSING-DOC] | `UpdateCategoryCommand` + handler exists in code |
| 43 | [MISSING-DOC] | `UpdateCategoryImageCommand` + handler exists in code |
| 44 | [MISSING-DOC] | `DeleteCategoryCommand` + handler (soft-delete/deactivate) exists in code |
| 45 | [MISSING-DOC] | `GetCategoryByIdQuery` + handler exists in code |
| 46 | [MISSING-DOC] | `ListCategoriesQuery` + handler exists in code |
| 47 | [MISSING-DOC] | `CategoryController` at `/api/v1/categories` with full CRUD + image upload |
| 48 | [MISSING-DOC] | `CategoryResponse` DTO (id, name, description, imageBase64, active, timestamps) |

### 1.8 Inter-Module API

| # | Type | Problem |
|---|------|---------|
| 49 | [CODE] | `ProductQueryApi.getById()` uses `UUID` not `ProductId` |
| 50 | [CODE] | `ProductInfo` has many more fields in code (serviceDescription, technicalDescription, monthlyPrice, annualPrice, currency, status, category, priority) than doc describes (id, name, description, price, currency) |
| 51 | [CODE] | Doc says consumers are "Order Management module" — code's actual consumer is Cart module |

### 1.9 Business Rules

| # | Type | Problem |
|---|------|---------|
| 52 | [CODE] | "Product name must be unique" rule — no unique constraint exists in code |
| 53 | [CODE] | Doc says "Price changes are audited via `PriceChanged` event" — event doesn't exist in code |

---

## 2. Cart Module

| # | Type | Problem |
|---|------|---------|
| 54 | [MISSING-DOC] | `guestToken` field exists in Cart code but is not documented |
| 55 | [MISSING-DOC] | `createForGuest(String guestToken)` factory exists but is not documented |
| 56 | [MISSING-DOC] | `mergeFrom(Cart otherCart)` method exists but is not documented |
| 57 | [MISSING-DOC] | `attachToUser(UUID userId)` method exists but is not documented |
| 58 | [MISSING-DOC] | `POST /api/v1/cart/merge` endpoint exists (returns 410 GONE, deprecated) but is not documented |
| 59 | [DOC-CONFLICT] | Cart doc says "An authenticated user has a single active cart" — code also supports guest carts. Decide if guest carts are part of the design or deprecated code to remove |

---

## 3. User Module

### 3.1 Commands

| # | Type | Problem |
|---|------|---------|
| 60 | [CODE] | `LogoutCommand` — doc says takes `userId: UUID` and revokes all tokens / code takes `refreshToken: String` and revokes only that one |
| 61 | [CODE] | `POST /api/v1/auth/logout` — doc says CUSTOMER role required / code is under `/api/v1/auth/**` which is `permitAll()` for POST |

### 3.2 Backoffice Admin Endpoints

| # | Type | Problem |
|---|------|---------|
| 62 | [MISSING-CODE] | `GET /api/v1/admin/users` (list all users) — no AdminController exists |
| 63 | [MISSING-CODE] | `GET /api/v1/admin/users/{id}` (get any user) — does not exist |
| 64 | [MISSING-CODE] | `PUT /api/v1/admin/users/{id}` (update user) — does not exist |
| 65 | [MISSING-CODE] | `POST /api/v1/admin/users/{id}/deactivate` — does not exist |

### 3.3 Domain Events & Handlers

| # | Type | Problem |
|---|------|---------|
| 66 | [MISSING-DOC] | `UserRegisteredListener` exists in code (sends welcome email via MailService) but is not documented in user-module.md |

### 3.4 JWT / Security

| # | Type | Problem |
|---|------|---------|
| 67 | [CODE] | `JwtProvider.validateToken()` — doc name vs code name `validateAccessToken()` |
| 68 | [MISSING-DOC] | `JwtProvider.getAccessTokenExpirationHours()` method not documented |
| 69 | [MISSING-DOC] | `JwtProvider.getRefreshTokenExpirationHours()` method not documented |
| 70 | [CODE] | JWT access token lifetime — doc says "15 minutes" / code uses configurable hours (`jwt.access-expiration-hours`) |
| 71 | [CODE] | `AuthResponse.expiresIn` — doc implies seconds / code returns hours |
| 72 | [MISSING-DOC] | JWT `iss` claim ("cyna-platform") not documented |
| 73 | [MISSING-DOC] | JWT `jti` claim (random UUID) not documented |

---

## 4. Bounded Contexts Document

| # | Type | Problem |
|---|------|---------|
| 74 | [DOC-CONFLICT] | IAM domain events list `UserActivated`, `PasswordChanged`, `RoleAssigned` — these don't exist in code and are not in user-module.md either |
| 75 | [DOC-CONFLICT] | Product Catalog description says "Category = endpoint, network, cloud" — product-module.md originally said the same, but code uses SOC, EDR, XDR |
| 76 | [DOC-CONFLICT] | Subscription billing cycle listed as "Monthly, quarterly, annual" — subscription-module.md BillingCycle enum only has `MONTHLY`, `ANNUAL` |
| 77 | [DOC-CONFLICT] | Product Catalog key aggregate only lists `Product` — Category is a separate first-class aggregate in code |
| 78 | [DOC-CONFLICT] | Context map says "Product info used by Cart and Order" — correct, but the product-module.md Dependencies section says consumer is "Order Management" (not Cart) |

---

## 5. Notification Module

| # | Type | Problem |
|---|------|---------|
| 79 | [DOC-CONFLICT] | Notification event handlers reference `PasswordChanged` and `UserActivated` events — these do not exist in user-module.md or code |
| 80 | [MISSING-CODE] | Entire notification module is documented but has zero code implementation |

---

## 6. Order Module

| # | Type | Problem |
|---|------|---------|
| 81 | [MISSING-CODE] | Entire order module is documented but has zero code implementation |

---

## 7. Payment Module

| # | Type | Problem |
|---|------|---------|
| 82 | [MISSING-CODE] | Entire payment module is documented but has zero code implementation |

---

## 8. Subscription Module

| # | Type | Problem |
|---|------|---------|
| 83 | [MISSING-CODE] | Entire subscription module is documented but has zero code implementation |

---

## 9. README.md

| # | Type | Problem |
|---|------|---------|
| 84 | [DOC-CONFLICT] | Module table does not list User or Cart modules |

---

## 10. SecurityConfig

| # | Type | Problem |
|---|------|---------|
| 85 | [CODE] | Product admin routes — SecurityConfig uses `/api/v1/products/**` with method-based filtering instead of `/api/v1/admin/products/**` prefix |
| 86 | [CODE] | Category admin routes — SecurityConfig uses `/api/v1/categories/**` with method-based filtering instead of `/api/v1/admin/categories/**` prefix |
| 87 | [CODE] | `GET /api/v1/account` is protected by `anyRequest().authenticated()` (any role) — doc says CUSTOMER role |
| 88 | [MISSING-DOC] | CORS configured for `http://localhost:4200` only — not documented |

---

## Priority Order

### Critical (design mismatch)
1. Product pricing model (#6) — single price vs dual price
2. Product fields (#3, #4) — description/features vs serviceDescription/technicalDescription
3. Product enum values (#5) — ENDPOINT/NETWORK/CLOUD vs SOC/EDR/XDR
4. Product status (#7) — missing DRAFT state
5. Routes prefix (#33, #85, #86) — /admin/ prefix not in code
6. Logout behavior (#60, #61) — different semantics

### High (missing implementations)
7. Product domain events (#14–18) — 5 events documented, 0 implemented
8. Product commands (#24–26) — publish/unpublish/changePrice not implemented
9. Category aggregate (#39–48) — full aggregate undocumented in product doc
10. User admin endpoints (#62–65) — 4 endpoints documented but no code
11. BillingCycle QUARTERLY (#8) — documented but not implemented

### Medium (4 planned modules)
12. Order module (#81)
13. Payment module (#82)
14. Subscription module (#83)
15. Notification module (#84)

### Low (minor discrepancies)
16. JWT configuration details (#67–73)
17. Cart guest token (#54–59)
18. README module list (#84)
