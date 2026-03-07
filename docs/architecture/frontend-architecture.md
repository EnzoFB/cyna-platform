# Frontend Architecture

## Purpose

This document describes the architecture of the two Angular 20 frontend applications in the CYNA Platform: the **Customer PWA** and the **Administrator Backoffice**.

---

## Applications

| Application | Purpose | Users | Key Features |
|-------------|---------|-------|--------------|
| **Customer PWA** | Self-service storefront | Customers | Browse services, subscribe, manage account, PWA installable |
| **Admin Backoffice** | Administration dashboard | Administrators | Manage products, orders, users, platform config |

Both applications share a common design system and foundational libraries, but are maintained as separate Angular projects within the monorepo.

---

## Monorepo Structure

```
cyna-frontend/
├── projects/
│   ├── pwa/                        # Customer-facing PWA
│   │   ├── src/
│   │   │   ├── app/
│   │   │   │   ├── core/           # Singletons, guards, interceptors
│   │   │   │   ├── features/       # Feature modules (lazy-loaded)
│   │   │   │   ├── shared/         # Shared components, pipes, directives
│   │   │   │   └── app.config.ts
│   │   │   ├── assets/
│   │   │   ├── environments/
│   │   │   └── manifest.webmanifest
│   │   └── ...
│   │
│   ├── backoffice/                 # Admin dashboard
│   │   ├── src/
│   │   │   ├── app/
│   │   │   │   ├── core/
│   │   │   │   ├── features/
│   │   │   │   ├── shared/
│   │   │   │   └── app.config.ts
│   │   │   ├── assets/
│   │   │   └── environments/
│   │   └── ...
│   │
│   └── ui-kit/                     # Shared design system library
│       ├── src/
│       │   ├── components/
│       │   ├── directives/
│       │   ├── pipes/
│       │   ├── styles/
│       │   └── public-api.ts
│       └── ...
│
├── angular.json
├── package.json
└── tsconfig.json
```

---

## Application Architecture

Each application follows a consistent internal architecture based on Angular best practices and the principle of feature isolation.

### Layers

```
┌─────────────────────────────────────────┐
│               Features                  │  ← Lazy-loaded feature modules
│  ┌───────────────────────────────────┐  │
│  │              Core                 │  │  ← Services, guards, interceptors
│  │  ┌───────────────────────────┐    │  │
│  │  │          Shared           │    │  │  ← Reusable components, pipes
│  │  └───────────────────────────┘    │  │
│  └───────────────────────────────────┘  │
│               UI Kit (library)          │  ← Design system
└─────────────────────────────────────────┘
```

### Core Module (`core/`)

Singleton services and app-wide concerns:

| Component | Responsibility |
|-----------|---------------|
| `AuthService` | JWT token management, login/logout |
| `AuthInterceptor` | Attach access token to HTTP requests |
| `RefreshTokenInterceptor` | Handle 401 and refresh token rotation |
| `AuthGuard` | Protect routes requiring authentication |
| `RoleGuard` | Protect routes based on user role |
| `ErrorInterceptor` | Global HTTP error handling |
| `LoadingInterceptor` | Global loading state management |
| `ApiService` | Base HTTP service with typed methods |

**Rules:**

- Core services are `providedIn: 'root'` (singleton).
- Core module is imported only once in `app.config.ts`.
- No UI components in core.

### Feature Modules (`features/`)

Each business capability is a lazy-loaded feature module:

```
features/
├── catalog/
│   ├── components/
│   │   ├── product-list/
│   │   ├── product-detail/
│   │   └── product-card/
│   ├── services/
│   │   └── catalog.service.ts
│   ├── models/
│   │   └── product.model.ts
│   ├── store/           # Feature-level state (signals/NgRx)
│   ├── catalog.routes.ts
│   └── index.ts
│
├── cart/
├── checkout/
├── account/
├── dashboard/          # (backoffice)
├── user-management/    # (backoffice)
└── ...
```

**Rules:**

- Each feature is self-contained (components, services, models, routes).
- Features are lazy-loaded via the Angular router.
- Features must not import other features directly.
- Cross-feature communication uses shared services or state management.

### Shared Module (`shared/`)

Reusable building blocks without business logic:

- Presentational components (cards, tables, modals)
- Pipes (date formatting, currency formatting)
- Directives (click-outside, infinite scroll)
- Utility functions

**Rules:**

- Shared components must be pure and stateless where possible.
- No feature-specific logic in shared.
- All shared items are exported via a barrel file.

### UI Kit Library (`ui-kit/`)

The design system library shared across both applications:

| Category | Examples |
|----------|---------|
| Atoms | Button, Input, Badge, Icon, Spinner |
| Molecules | Form Field, Search Bar, Notification Toast |
| Organisms | Data Table, Sidebar Nav, Header |
| Tokens | Colors, typography, spacing (CSS custom properties) |

**Rules:**

- UI Kit has zero business logic.
- UI Kit is published as an Angular library.
- All components support accessibility (ARIA attributes, keyboard navigation).
- Components use `OnPush` change detection.

---

## State Management

### Approach

Angular 20 **Signals** are the primary state management mechanism for local and component state. For complex cross-feature state, **NgRx Signal Store** is used.

| Scope | Mechanism |
|-------|-----------|
| Component-local | Angular Signals |
| Feature-level | NgRx Signal Store (feature store) |
| App-wide (auth, user) | Singleton service with Signals |

### Rules

- Avoid storing server state in client stores — prefer fetching and caching via HTTP.
- Use `computed()` for derived state.
- Side effects (HTTP calls) live in services, not in stores directly.

---

## Routing

### PWA Routes

```
/                           → Home / Landing
/catalog                    → Service catalog
/catalog/:id                → Service detail
/cart                       → Shopping cart
/checkout                   → Checkout flow
/account                    → Account dashboard
/account/subscriptions      → Active subscriptions
/account/orders             → Order history
/auth/login                 → Login
/auth/register              → Registration
```

### Backoffice Routes

```
/dashboard                  → Admin dashboard
/products                   → Product management
/products/:id               → Product detail/edit
/orders                     → Order management
/orders/:id                 → Order detail
/users                      → User management
/users/:id                  → User detail
/settings                   → Platform settings
```

### Route Guards

| Guard | Purpose |
|-------|---------|
| `AuthGuard` | Redirects unauthenticated users to `/auth/login` |
| `RoleGuard` | Checks user role matches required role for the route |
| `GuestGuard` | Redirects authenticated users away from auth pages |

---

## API Communication

### HTTP Service Pattern

All API calls go through typed service methods:

```typescript
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  getProducts(params: ProductSearchParams): Observable<PagedResponse<ProductDto>> {
    return this.http.get<PagedResponse<ProductDto>>(
      `${this.apiUrl}/api/v1/products`,
      { params: toHttpParams(params) }
    );
  }

  getProductById(id: string): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${this.apiUrl}/api/v1/products/${id}`);
  }
}
```

### Interceptors

Interceptors handle cross-cutting concerns:

1. **AuthInterceptor** — Adds `Authorization: Bearer <token>` header.
2. **RefreshTokenInterceptor** — On 401, refreshes the token and retries the request.
3. **ErrorInterceptor** — Maps HTTP errors to user-friendly notifications.
4. **LoadingInterceptor** — Tracks pending requests for global loading indicators.

---

## PWA Features

The Customer application is a Progressive Web App:

| Feature | Implementation |
|---------|---------------|
| Installable | `manifest.webmanifest` with icons, theme color |
| Service Worker | Angular service worker for asset caching |
| Offline support | Cache-first strategy for static assets, network-first for API |
| Push notifications | Web Push API (future) |
| Responsive design | Mobile-first, CSS Grid/Flexbox, breakpoints in UI Kit |

---

## Build and Deployment

| Concern | Tool |
|---------|------|
| Build | Angular CLI (`ng build`) |
| Bundling | Esbuild (Angular 20 default) |
| Linting | ESLint + Angular ESLint |
| Formatting | Prettier |
| Testing | Jest (unit), Cypress (e2e) |
| CI | GitHub Actions |

### Build Commands

```bash
# Customer PWA
ng build pwa --configuration=production

# Backoffice
ng build backoffice --configuration=production

# UI Kit
ng build ui-kit
```

---

## Best Practices

1. **Standalone components** — All components are standalone (Angular 20 default). Do not use NgModules for components.
2. **OnPush change detection** — All components use `ChangeDetectionStrategy.OnPush`.
3. **Typed forms** — Use Angular typed reactive forms exclusively.
4. **Strict TypeScript** — `strict: true` in `tsconfig.json`. No `any` types.
5. **Barrel exports** — Every feature and shared module has an `index.ts`.
6. **Lazy loading** — All feature routes are lazy-loaded.
7. **Accessibility** — All interactive elements have ARIA labels and keyboard support.
8. **Internationalization** — Use Angular i18n or `@ngx-translate` for all user-visible text.

---

## Common Mistakes to Avoid

| Mistake | Correct Approach |
|---------|-----------------|
| Importing one feature into another | Use shared services or state management |
| Business logic in components | Move logic to services |
| Using `any` type | Define proper interfaces/types |
| Subscribing in components without cleanup | Use `async` pipe or `takeUntilDestroyed()` |
| Hardcoding API URLs | Use environment configuration |
| Storing secrets in frontend code | Never store secrets client-side |
| Ignoring loading/error states | Always handle all three states (loading, success, error) |

---

## Related Documents

- [Architecture Overview](architecture-overview.md)
- [API Guidelines](../api/api-guidelines.md)
- [Authentication](../security/authentication.md)
