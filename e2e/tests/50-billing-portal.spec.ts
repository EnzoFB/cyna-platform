import { test, expect } from '@playwright/test';
import { registerUser, RegisteredUser, API_URL } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart, clickCheckoutFromCart } from '../helpers/cart';
import { fillBillingForm, fillStripeCard, submitCheckout, TEST_CARDS } from '../helpers/checkout';
import { listSubscriptions, waitFor } from '../helpers/backend';
import { request as pwRequest } from '@playwright/test';

/**
 * Tests for the Stripe Customer Portal integration.
 *
 * The "subscriber can open" test is split into two complementary checks:
 *  1) UI wiring: the button is visible, calls the API, and triggers a top-level
 *     navigation. We stub the backend response so we don't actually leave the
 *     test app for billing.stripe.com.
 *  2) Real backend: a direct API call on the same authenticated user, asserting
 *     the backend produces a real https://billing.stripe.com/... URL.
 *
 * This separation avoids races between page navigation and reading the response
 * body, which Playwright cannot do once the page is unloading.
 */
test.describe('Account — Stripe Customer Portal', () => {
  test('subscriber sees the portal button + UI wiring works + real Stripe URL is generated', async ({ page }) => {
    test.setTimeout(180_000);

    // 1. Register, buy something so the user has a Stripe customer + subscription
    const user: RegisteredUser = await registerUser('-portal');
    await loginViaToken(page, user);
    await addProductToCart(page, { billingCycle: 'MONTHLY' });
    await goToCart(page);
    await clickCheckoutFromCart(page);
    await fillBillingForm(page);
    await fillStripeCard(page, TEST_CARDS.ok);
    await submitCheckout(page);
    await page.waitForURL(/.*\/checkout\/success\//, { timeout: 60_000 });

    // 2. Wait via API until the webhook has created the local subscription.
    await waitFor(
      () => listSubscriptions(user),
      (subs) => subs.length >= 1,
      { timeoutMs: 30_000, intervalMs: 1_500, label: 'subscription created via webhook' }
    );

    // 3. Validate the real backend produces a Stripe-hosted URL (no UI involved).
    await test.step('Real backend returns a billing.stripe.com URL', async () => {
      const ctx = await pwRequest.newContext();
      const res = await ctx.post(`${API_URL}/payments/billing-portal`, {
        headers: { Authorization: `Bearer ${user.accessToken}` },
        data: { returnUrl: 'http://localhost:4200/account/subscriptions' },
      });
      expect(res.status(), `expected 200 but got ${res.status()}: ${await res.text()}`).toBe(200);
      const body = await res.json();
      expect(body.success).toBe(true);
      expect(body.data.url).toMatch(/^https:\/\/billing\.stripe\.com\//);
      await ctx.dispose();
    });

    // 4. Validate the UI wiring without ever leaving the test app: stub the backend
    //    response with a sentinel URL and assert the page navigates to it.
    await test.step('Clicking the button triggers navigation to the returned URL', async () => {
      await page.goto('/account/subscriptions');
      await expect(page.locator('.my-sub-card').first()).toBeVisible({ timeout: 10_000 });

      const SENTINEL_URL = 'http://localhost:4200/billing-portal-sentinel?ok=1';
      await page.route('**/api/v1/payments/billing-portal', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { url: SENTINEL_URL },
            error: null,
            timestamp: new Date().toISOString(),
          }),
        });
      });

      await page.getByRole('button', { name: /paiements et factures|payments and invoices/i }).click();

      // The frontend does window.location.href = url → page navigates away from
      // /account/subscriptions to the sentinel URL.
      await page.waitForURL(/billing-portal-sentinel/, { timeout: 10_000 });
      expect(page.url()).toBe(SENTINEL_URL);
    });
  });

  test('without any subscription, the portal button is hidden', async ({ page }) => {
    const user = await registerUser('-portal-empty');
    await loginViaToken(page, user);

    await page.goto('/account/subscriptions');
    await expect(page.locator('.my-subs-empty')).toBeVisible({ timeout: 10_000 });
    await expect(
      page.getByRole('button', { name: /paiements et factures|payments and invoices/i })
    ).toHaveCount(0);
  });
});
