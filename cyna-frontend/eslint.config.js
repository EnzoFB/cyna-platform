// @ts-check
const eslint = require("@eslint/js");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");

module.exports = tseslint.config(
  {
    ignores: [
      "dist/**",
      "node_modules/**",
      "coverage/**",
      ".angular/**",
      "**/*.config.js",
      "**/karma.conf.js",
    ],
  },
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...tseslint.configs.stylistic,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    // Soft baseline: noisy stylistic / migration rules are warnings so the
    // CI gate isn't blocked on day one. Tighten to `error` over time as the
    // team paydown progresses.
    rules: {
      "@typescript-eslint/no-explicit-any": "warn",
      "@typescript-eslint/no-inferrable-types": "warn",
      "@typescript-eslint/no-empty-function": "warn",
      "@angular-eslint/prefer-inject": "warn",
      "@angular-eslint/no-output-native": "warn",
    },
  },
  {
    files: ["projects/pwa/**/*.ts", "projects/backoffice/**/*.ts"],
    rules: {
      "@angular-eslint/directive-selector": [
        "error",
        { type: "attribute", prefix: "app", style: "camelCase" },
      ],
      "@angular-eslint/component-selector": [
        "error",
        { type: "element", prefix: "app", style: "kebab-case" },
      ],
    },
  },
  {
    files: ["projects/ui-kit/**/*.ts"],
    rules: {
      "@angular-eslint/directive-selector": [
        "error",
        { type: "attribute", prefix: "lib", style: "camelCase" },
      ],
      "@angular-eslint/component-selector": [
        "error",
        { type: "element", prefix: "lib", style: "kebab-case" },
      ],
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility,
    ],
    // Same soft-baseline policy for accessibility rules — surface them as
    // warnings until the existing violations are paid down, then promote.
    rules: {
      "@angular-eslint/template/click-events-have-key-events": "warn",
      "@angular-eslint/template/interactive-supports-focus": "warn",
      "@angular-eslint/template/label-has-associated-control": "warn",
      "@angular-eslint/template/role-has-required-aria": "warn",
    },
  },
);
