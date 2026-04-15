# Frontend Design System

## Objective

This document defines the baseline rules for UI styling in the Angular frontends (PWA and backoffice) to keep a consistent SaaS visual language while allowing incremental delivery.

## Scope

- Colors, typography, spacing, radii and shadows
- Shared primitives (`button`, `card`, `input shell`)
- Interactive states (`hover`, `focus-visible`, `disabled`, `error`)
- Progressive migration rules for existing components

## Source of truth

- Tokens: `cyna-frontend/projects/pwa/src/styles/_tokens.scss`
- Primitive classes: `cyna-frontend/projects/pwa/src/styles/_primitives.scss`
- Color palette: `cyna-frontend/projects/pwa/src/styles/_colors.scss`
- Global styles entrypoint: `cyna-frontend/projects/pwa/src/styles.scss`

## Tokens

Use CSS custom properties from `:root`:

- Colors: `--ds-color-*`
- Radius: `--ds-radius-*`
- Spacing: `--ds-space-*`
- Shadows: `--ds-shadow-*`

Hardcoded hex values in component styles are discouraged unless there is a justified, component-specific exception.

## Shared primitives

Use these primitives before adding new component-specific styles:

- `.ds-card`: shared surface container
- `.ds-btn`, `.ds-btn--primary`, `.ds-btn--dark`
- `.ds-input-shell`

Rules:

1. Keep local component classes for layout and structure.
2. Add `ds-*` classes for visual foundation.
3. Override locally only when needed by business UX.

## Accessibility and states

- Keep visible focus ring (`:focus-visible`) on all interactive controls.
- Do not remove outlines without an equivalent accessible replacement.
- Keep disabled state readable (`opacity + cursor`) and avoid color-only feedback for critical errors.

## Naming conventions

- Prefer BEM for component-specific classes.
- Use `ds-*` prefix for cross-component design primitives.
- Avoid ambiguous utility names (`.primary-btn`, `.box`, `.item`) for new code.

## Migration strategy

To avoid risky big-bang refactors:

1. New feature: use tokens and primitives by default.
2. Existing feature touched by a ticket: migrate only edited blocks.
3. Validate with existing tests/build after each migration step.

## Review checklist

Before merging a frontend ticket:

1. Are tokens used instead of hardcoded colors/radius/spacing?
2. Are shared primitives reused where applicable?
3. Are focus-visible and disabled states consistent?
4. Is there no unnecessary CSS duplication?
5. Does build/test still pass?

