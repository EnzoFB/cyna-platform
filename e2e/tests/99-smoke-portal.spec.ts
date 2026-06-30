/**
 * Quick smoke test for the billing-portal endpoint, isolated from the UI flow.
 * This file is intentionally numbered 99 so it runs last; it can be deleted
 * once the full 50-billing-portal e2e is validated.
 */
import { test, expect, request } from '@playwright/test';
import { registerUser, API_URL } from '../helpers/api';

test('billing portal endpoint returns NO_STRIPE_CUSTOMER for a fresh user', async () => {
  const user = await registerUser('-smoke-portal');

  const ctx = await request.newContext();
  const res = await ctx.post(`${API_URL}/payments/billing-portal`, {
    headers: { Authorization: `Bearer ${user.accessToken}` },
    data: { returnUrl: 'http://localhost:4200/account/subscriptions' },
  });

  expect(res.status()).toBe(404);
  const body = await res.json();
  expect(body.success).toBe(false);
  expect(body.error.code).toBe('NO_STRIPE_CUSTOMER');
  await ctx.dispose();
});

test('billing portal endpoint rejects missing returnUrl', async () => {
  const user = await registerUser('-smoke-portal-no-url');

  const ctx = await request.newContext();
  const res = await ctx.post(`${API_URL}/payments/billing-portal`, {
    headers: { Authorization: `Bearer ${user.accessToken}` },
    data: {},
  });

  // Bean validation kicks in → 400
  expect(res.status()).toBe(400);
  await ctx.dispose();
});

test('billing portal endpoint rejects unauthenticated requests', async () => {
  const ctx = await request.newContext();
  const res = await ctx.post(`${API_URL}/payments/billing-portal`, {
    data: { returnUrl: 'http://localhost:4200/account/subscriptions' },
  });
  expect(res.status()).toBe(401);
  await ctx.dispose();
});
