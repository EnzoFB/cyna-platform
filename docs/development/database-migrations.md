# Database Migrations — Conventions & Workflow

This document is the source of truth for how the team manages Flyway migrations on
the `cyna-backend` Modular Monolith. Read it end-to-end before adding, renaming,
or rolling back a migration.

## Rules (non-negotiable)

1. **Migrations are append-only.** Once a migration is merged on `develop`, it is
   immutable: you do **not** edit, rename, or delete it. If a previously merged
   migration is wrong, write a *new* migration that corrects the schema.
   Editing an applied migration corrupts Flyway's `flyway_schema_history`
   checksum and breaks every environment that has already run it.

2. **One Flyway version per file. Ever.** Two files with the same `V{n}` prefix
   (e.g. `V3__foo.sql` and `V3__bar.sql`) make Flyway refuse to start with
   "Found more than one migration with version 3". The CI job
   `check-migrations` blocks PRs that violate this — but you should also run
   the script locally before pushing (see below).

3. **Migrations target the right schema.** Each module owns its own schema:
   `user_schema`, `product_schema`, `cart_schema`, `order_schema`,
   `subscription_schema`, `payment_schema`. Always fully qualify table names
   (e.g. `subscription_schema.subscriptions`, never just `subscriptions`).
   Cross-schema FK constraints are allowed but should be exceptional.

4. **No `flywayClean` in non-dev environments.** The Gradle `flyway` task is
   wired to localhost only; on staging and prod, migrations run from
   Spring Boot startup. Never expose `flywayClean` to a deployed environment.

5. **One logical change per migration.** Easier to review, easier to rollback,
   easier to bisect. Splitting "add column + backfill + add index" into three
   files is preferred over one mega-migration.

## File naming

Format: `V{n}__{snake_case_description}.sql`

- `{n}` is a positive integer, monotonically increasing.
- The description is lowercase snake_case, no dashes, no uppercase.
- Verbs: prefer `add_`, `create_`, `drop_`, `rename_`, `backfill_`.

Examples:

```
V11__create_login_otp_challenges_table.sql        ✅
V12__add_subscription_idempotency_key.sql         ✅
V13__backfill_orders_currency_to_eur.sql          ✅
V11__addLoginOtp.sql                              ❌ camelCase
V11_create-login-otp.sql                          ❌ dashes & single underscore
```

## Picking the next version number

Run this from the repo root before adding a migration:

```bash
ls cyna-backend/src/main/resources/db/migration/V*.sql \
  | sed -E 's/.*V([0-9]+)__.*/\1/' \
  | sort -n | tail -1
```

The next migration is `V{that+1}`. **Always reserve your number on the team
channel before pushing your branch** — this is the cheapest collision avoidance
mechanism short of switching numbering schemes (see "Future" below).

## Local pre-push check

```bash
bash cyna-backend/scripts/check-flyway-migrations.sh
```

This catches duplicate versions, naming violations, and empty files. The same
script runs in CI (`check-migrations` job).

## Workflow when adding a migration

1. **Reserve the version number** (Slack/issue tracker) — avoids collision
   with a teammate working on a parallel feature.
2. **Create the file** with the correct `V{n}__name.sql` prefix.
3. **Write the SQL** — use `IF NOT EXISTS` / `IF EXISTS` for idempotence
   where it makes sense (table creation, index creation).
4. **Update the matching JPA entity** in the same commit. Hibernate runs with
   `ddl-auto: validate` — drift between entity and schema fails startup.
5. **Apply locally** with `./gradlew flywayMigrate`.
6. **Boot the app** to confirm `ddl-auto: validate` is happy.
7. **Run the local migration check** (above).
8. **Commit & push.** CI will validate again; the boot smoke test (`boot-backend`
   job) re-applies all migrations against a fresh Postgres.

## Workflow when a collision is merged on `develop`

This is the situation we want to never happen, but if it does:

1. **Stop merging into `develop`** until it is fixed (announce in team channel).
2. **Identify the older migration** via `git log --diff-filter=A` on the file —
   the one merged earliest keeps its version number.
3. **Open a dedicated `fix/migration-vX-collision` branch** off `develop`. Do
   not bundle the fix into a feature PR.
4. **Renumber the newer migration** to the next free slot. The PR should be
   small and easy to review.
5. **Coordinate the dev DB reset.** Every developer (and any shared
   non-prod environment) that already applied the old version must reset
   their local DB:

   ```bash
   ./gradlew flywayClean    # destructive, dev only
   ./gradlew flywayMigrate
   ```

6. **If the collision reached an environment we cannot reset** (staging with
   real data, or worse — prod), use `flyway repair` to update the history
   table after renaming. See
   <https://documentation.red-gate.com/flyway/flyway-cli-and-api/commands/repair>.
   This must be done by someone with DB admin access, after a backup, and is
   never the first option.
7. **Post-mortem.** File an issue describing what failed in the process so
   the team can harden it (more guardrails, branch protection rules, etc.).

## What CI does for us

Two jobs, both must pass before merge:

- **`check-migrations`** (~5 seconds) — static check: no duplicate versions,
  naming convention respected, no empty files. Fast feedback, runs on every PR.
- **`boot-backend`** (~3 minutes) — boots Spring Boot against a fresh Postgres
  16, runs all migrations, hits `/actuator/health`. Catches checksum drift,
  `ddl-auto: validate` mismatches, broken FKs, and any migration that fails
  to apply for any reason.

Neither job replaces local verification. Run the local check + a `bootRun`
before pushing.

## Why we use `V{n}` and not timestamps (yet)

Pros of timestamp-based naming (`V202605041430__add_subscription_fk.sql`):

- Collisions are virtually impossible (would require two devs writing in the
  same minute).
- Files sort chronologically.

Cons (today):

- One-shot migration of `V1` … `V11` to timestamp format requires every
  developer and every non-prod environment to reset.
- Reviewers lose the visual cue that "V12 is right after V11".

We have decided to keep `V{n}` for now and rely on the CI guardrails + the
"reserve before pushing" convention. **Revisit this decision** if we hit a
second collision incident, or once the team grows past 5 active backend devs.

## Reference: the V3 collision incident (2026-04-27 → 2026-05-04)

Two migrations were merged into `develop` with version `V3`:

- `V3__add_company_and_email_change_tokens.sql` (2026-04-22, account/profile)
- `V3__create_login_otp_challenges_table.sql` (2026-04-27, 2FA)

Neither author noticed because (a) each branched from `develop` *before* the
other migration existed, (b) no CI job checked migration uniqueness, and
(c) `./gradlew build -x test` does not run Flyway. The collision blocked the
backend from booting on any fresh database. Resolution: the OTP migration
was renumbered to `V11` (it depends only on `user_schema.users` from `V1`,
so its position in the chain does not matter), and this guardrail
documentation + CI script were added in the same fix.
