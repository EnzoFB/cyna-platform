#!/usr/bin/env bash
# Validate Flyway migrations:
#   1. No two files share the same version (V{n}).
#   2. Every file matches the V{n}__{snake_case}.sql convention.
#   3. No file is empty.
#
# Run locally with `./cyna-backend/scripts/check-flyway-migrations.sh`
# or in CI before any build step. Exits non-zero on the first violation.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATIONS_DIR="${SCRIPT_DIR}/../src/main/resources/db/migration"

if [[ ! -d "$MIGRATIONS_DIR" ]]; then
  echo "ERROR: migrations directory not found at $MIGRATIONS_DIR" >&2
  exit 2
fi

cd "$MIGRATIONS_DIR"

shopt -s nullglob
files=( V*.sql )
shopt -u nullglob

if [[ ${#files[@]} -eq 0 ]]; then
  echo "ERROR: no Flyway migration files found in $MIGRATIONS_DIR" >&2
  exit 2
fi

errors=0

# 1. Naming convention: V{n}__{snake_case}.sql
naming_re='^V[0-9]+__[a-z0-9_]+\.sql$'
for f in "${files[@]}"; do
  if [[ ! "$f" =~ $naming_re ]]; then
    echo "ERROR: invalid migration name '$f' (expected V{n}__{snake_case}.sql)" >&2
    errors=$((errors + 1))
  fi
done

# 2. Duplicate versions
duplicates=$(printf '%s\n' "${files[@]}" \
  | sed -E 's/^(V[0-9]+)__.*/\1/' \
  | sort \
  | uniq -d)

if [[ -n "$duplicates" ]]; then
  echo "ERROR: duplicate Flyway versions detected:" >&2
  while IFS= read -r ver; do
    [[ -z "$ver" ]] && continue
    echo "  $ver:" >&2
    for f in "${files[@]}"; do
      [[ "$f" == "${ver}__"* ]] && echo "    - $f" >&2
    done
  done <<< "$duplicates"
  errors=$((errors + 1))
fi

# 3. Empty files
for f in "${files[@]}"; do
  if [[ ! -s "$f" ]]; then
    echo "ERROR: migration '$f' is empty" >&2
    errors=$((errors + 1))
  fi
done

if [[ $errors -gt 0 ]]; then
  echo "" >&2
  echo "Flyway migration check failed ($errors issue(s))." >&2
  exit 1
fi

echo "Flyway migration check passed (${#files[@]} files)."
