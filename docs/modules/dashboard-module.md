# Dashboard Module

> **Status: implemented.**
> This document describes the module **as built**. The dashboard owns no
> business data of its own — it is a reporting facade that orchestrates the
> reporting APIs of `order`, `user` and `subscription`. Its only persisted
> state is the per-year goal settings (revenue target, clients target,
> monthly revenue breakdown) configured by the admin.

## Purpose

The **dashboard** module backs the backoffice home screen for administrators.
It answers a single product question: *"how is the business doing this fiscal
year, against the goals we set, compared to last week/month/quarter?"*

It is a **coordination module** in the spirit of `notification` and `account`:
it owns the *reporting bounded context* but holds **no transactional data**.
Every business figure (revenue, sales count, customer count, active
subscriptions, top products, monthly revenue, orders by status) is computed by
the module that owns the data, over its own schema, and returned through that
module's published `application.api`.

| Property | Value |
|----------|-------|
| Base package | `com.cyna.modules.dashboard` |
| Database schema | `dashboard_schema` (one table: `dashboard_goal_settings`) |
| Layers present | `application`, `domain`, `infrastructure`, `interfaces` |
| Audience | backoffice admins only (`hasRole('ADMIN')`) |

## Dependency direction (why it looks the way it does)

The defining constraint is **one-way dependencies**, enforced by
`ModuleIsolationRulesTest` (no module cycles — the codebase must stay
split-ready):

```
dashboard  ───►  order / user / subscription
   (depends on their application.api only)

nobody depends on dashboard
```

The dashboard imports **only** the `application.api` packages of the other
modules — never their `domain`, `infrastructure` or `interfaces`. It does
**not** read another module's tables: `SchemaIsolationRulesTest` guarantees
that the only `*_schema.` token allowed inside `modules/dashboard/` is
`dashboard_schema`. At a microservice split, the three injected `*QueryApi`
beans become remote calls — the boundary is already the `application.api`
seam.

## Architecture

```
modules/dashboard/
  application/
    command/
      updategoaltarget/
        DashboardGoalKey.java                              # REVENUE | CLIENTS
        UpdateDashboardGoalTargetCommand.java
        UpdateDashboardGoalTargetCommandHandler.java
      updatemonthlyrevenuegoal/
        UpdateDashboardMonthlyRevenueGoalsCommand.java
        UpdateDashboardMonthlyRevenueGoalsCommandHandler.java
    query/
      getdashboard/
        AdminDashboardQueryPort.java                       # the cross-module read seam
        AdminDashboardReadModelAssembler.java              # builds the dashboard from the port
        GetAdminDashboardQuery.java
        GetAdminDashboardQueryHandler.java
        AdminDashboardReadModel.java                       # response root
        DashboardYearReadModel.java                        # one entry per fiscal year
        DashboardMetricReadModel.java                      # value + flow comparisons
        DashboardComparisonReadModel.java                  # week / month / quarter
        DashboardGoalReadModel.java                        # inProgress / target
        DashboardTopProductReadModel.java
        MonthlyRevenueAggregate.java
        TopProductAggregate.java
  domain/
    model/DashboardGoalSettings.java                       # value object, normalises monthly array to 12 cells
    repository/DashboardGoalSettingsRepository.java
  infrastructure/
    persistence/
      entity/DashboardGoalSettingsJpaEntity.java
      repository/
        JpaDashboardGoalSettingsRepositoryAdapter.java
        SpringDataDashboardGoalSettingsRepository.java
        ModuleApiAdminDashboardQueryAdapter.java           # AdminDashboardQueryPort impl — wires order/user/subscription QueryApi
  interfaces/
    rest/AdminDashboardController.java                     # /api/v1/admin/dashboard
```

- **`AdminDashboardQueryPort`** (application) is the *cross-module read seam*.
  It declares every datum the dashboard needs (sums, counts, monthly buckets,
  top products, available years), expressed in the dashboard's own vocabulary.
- **`ModuleApiAdminDashboardQueryAdapter`** (infrastructure) is the only
  implementation: each port method delegates 1-for-1 to a method of
  `OrderQueryApi`, `UserQueryApi` or `SubscriptionQueryApi`. There is no
  custom SQL in this module, no JOIN across schemas, and no read of another
  module's repository.
- **`AdminDashboardReadModelAssembler`** is the heart of the module. It takes
  the raw aggregates from the port and computes the *derived* figures the UI
  needs: percentage deltas against rolling 7-/30-/90-day windows, padded
  12-month arrays, default targets when no goal is yet set for the year,
  rescaling of the monthly breakdown when the year target changes.
- **`DashboardGoalSettings`** is a pure value object. It normalises the
  monthly array to exactly 12 non-negative cells, rescales the breakdown when
  the year target is updated, and distributes the target evenly when the
  caller has no preference.

## REST endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET`  | `/api/v1/admin/dashboard` | `ADMIN` | Returns the dashboard. With no `year` query param, returns every available year. With `?year=YYYY`, returns just that year (and adds it to `availableYears` if missing). |
| `PUT`  | `/api/v1/admin/dashboard/{year}/goals/target` | `ADMIN` | Updates the **revenue** or **clients** target for the year. Body: `{ goalKey: "revenue" \| "clients", targetValue: number }`. Updating `revenue` rescales the monthly breakdown to match the new total. |
| `PUT`  | `/api/v1/admin/dashboard/{year}/goals/monthly-revenue` | `ADMIN` | Sets the 12-month revenue breakdown directly. Body: `{ monthlyRevenueGoal: number[12] }`. The yearly revenue target becomes the sum of the array. |

All three return the recomputed `DashboardYearReadModel`. Command endpoints
return `400 VALIDATION_ERROR` on bad input (negative year/target, unknown
goal key, missing array).

### Response shape

```jsonc
{
  "currentYear": 2026,
  "availableYears": [2024, 2025, 2026],
  "years": [
    {
      "year": 2026,
      "metrics": {
        "revenue":             { "value": 12345600, "comparisons": { "week": +4.2, "weekValue": 11800000, "month": +12.0, "monthValue": 11000000, "quarter": +30.5, "quarterValue":  9450000 } },
        "clients":             { "value":      820, "comparisons": { "week":  ...,  "weekValue":  ..., "month": ..., "monthValue": ..., "quarter": ..., "quarterValue": ... } },
        "sales":               { "value":     3140, "comparisons": { ... } },
        "activeSubscriptions": { "value":      612, "comparisons": { ... } }
      },
      "revenueGoal":      { "inProgressValue": 12345600, "targetValue": 20000000 },
      "newClientsGoal":   { "inProgressValue":      150, "targetValue":      200 },
      "monthlyRevenueGoal":   [1660000, 1660000, ... ],   // 12 cells, sums to revenueGoal.targetValue
      "monthlyRevenueActual": [1582300, 1701400, ... ],   // 12 cells
      "topProducts":   [ { "id": "...", "name": "SOC Pro", "salesCount": 312, "revenueAmount": 5460000 }, ... ],
      "ordersByStatus": { "PENDING": 12, "PAID": 1840, "CANCELLED": 23 }
    }
  ]
}
```

- All amounts are integers in the smallest currency unit (centimes), the same
  representation used by `order` and `payment`.
- `comparisons.week / .month / .quarter` are **percentage deltas** (one decimal,
  e.g. `+4.2`, `-3.1`) against the matching rolling window of the same length
  ending one window earlier. `weekValue` / `monthValue` / `quarterValue` are
  the **absolute previous-window values** (revenue/sales/clients flow over
  that window, or active subscriptions at the start of the window for the
  stock metric).
- For past years, the anchor is the last instant of the year (so the
  comparison windows still make sense); for the current year, it is *now*.
- `activeSubscriptions` is a *stock* (point-in-time count), the three others
  are *flows* (delta over a window). The assembler uses two different
  comparison strategies accordingly.

## Goal settings — invariants

Persisted by `DashboardGoalSettings` (one row per fiscal year):

- `fiscalYear > 0`, primary key.
- `revenueTargetValue >= 0`, `clientsTargetValue >= 0`.
- `monthlyRevenueGoal.size() == 12`, all cells `>= 0`. The value object
  normalises any input list to exactly 12 non-negative `Long`s.
- After `withRevenueTargetValue(t)`: the monthly array is **rescaled** so its
  sum equals `t` (any rounding delta is absorbed by the last month).
- After `withMonthlyRevenueGoal(arr)`: the yearly target becomes
  `sum(arr)` — the two are kept in sync.

When no goal row exists yet for the requested year, the assembler computes a
**provisional target** of `1.2 × max(progress so far, sum of monthly actuals)`
for revenue, and `1.2 × clients so far` for new clients, then distributes the
revenue target evenly across the 12 months. This is only used to render the
dashboard; it is **not** persisted until the admin saves a target through one
of the `PUT` endpoints.

## Persistence

Single table `dashboard_schema.dashboard_goal_settings` (Flyway `V1`):

| Column | Type | Notes |
|--------|------|-------|
| `fiscal_year` | `INTEGER` PK | `CHECK (>= 2000)` |
| `revenue_target_value` | `NUMERIC(19,4)` | `CHECK (>= 0)` |
| `clients_target_value` | `BIGINT` | `CHECK (>= 0)` |
| `monthly_revenue_goal` | `JSONB` (12-element array) | default `'[]'::jsonb` |
| `created_at`, `updated_at` | `TIMESTAMPTZ` | `updated_at` maintained by trigger |

No foreign keys, no cross-schema reference.

## Known limitations

- **No real-time push.** The dashboard is a pull endpoint. The UI re-fetches
  on navigation or manual refresh; there is no WebSocket/SSE feed.
- **No caching layer.** Each `GET /admin/dashboard` recomputes every metric
  for every year requested by calling the three `*QueryApi` beans. Acceptable
  today (low admin traffic, in-process Java calls). At a microservice split
  this will need a cache or materialized projection — currently planned as a
  follow-up, not part of this module.
- **No drill-down endpoints.** The module exposes aggregates only. Detailed
  per-order or per-customer views live in the producing modules
  (`/api/v1/admin/orders`, `/api/v1/admin/users`), not here.
- **`locale` parameter on `findTopProductsByYear` is ignored.** Top-product
  names are read from the snapshot stored on the order line at purchase time,
  not from `product_schema` — so the dashboard never crosses into the product
  module's data. The locale argument is kept on the port signature for now
  but not used.

## Related documents

- [Bounded Contexts](../domain/bounded-contexts.md)
- [Inter-Module Communication](../architecture/inter-module-communication.md)
- [Dependency Rules](../architecture/dependency-rules.md)
- [Architecture Tests](../testing/architecture-tests.md)
