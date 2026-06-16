export const environment = {
  production: true,
  // Relative path — the reverse proxy (nginx) serving this SPA forwards
  // /api/v1/* to the backend, so the browser never needs an absolute host.
  apiUrl: '/api/v1',
  // Stripe PUBLISHABLE key (pk_live_…). Publishable keys are meant to be
  // embedded in front-end code, but the LIVE value must NOT be committed.
  // The frontend Docker build replaces the `__STRIPE_PUBLISHABLE_KEY__`
  // token below from the STRIPE_PUBLISHABLE_KEY build-arg (see
  // cyna-frontend/Dockerfile). For a non-Docker build, edit it manually.
  stripePublishableKey: '__STRIPE_PUBLISHABLE_KEY__',
};
