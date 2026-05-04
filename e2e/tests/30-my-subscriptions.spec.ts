import { test, expect } from '@playwright/test';
import { registerUser, RegisteredUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart, clickCheckoutFromCart } from '../helpers/cart';
import {
  fillBillingForm,
  fillStripeCard,
  submitCheckout,
  TEST_CARDS,
} from '../helpers/checkout';
import { listSubscriptions, waitFor } from '../helpers/backend';

/**
 * Validates the complete "My subscriptions" customer self-service flow:
 *  1. Customer buys a monthly subscription (webhook fires → Subscription locale ACTIVE)
 *  2. /account/subscriptions lists the active subscription with all details
 *  3. Customer cancels via the UI → confirmation modal → backend POST + Stripe sub cancel
 *  4. Subscription is reflected as CANCELLED in the UI
 */
test.describe('My subscriptions — customer self-service', () => {
  test('customer can view and cancel an active subscription', async ({ page }) => {
    test.setTimeout(180_000);
    let user!: RegisteredUser;

    await test.step('Customer purchases a monthly subscription', async () => {
      user = await registerUser('-mysubs');
      await loginViaToken(page, user);
      await addProductToCart(page, { billingCycle: 'MONTHLY' });
      await goToCart(page);
      await clickCheckoutFromCart(page);
      await fillBillingForm(page);
      await fillStripeCard(page, TEST_CARDS.ok);
      await submitCheckout(page);
      // Stripe sometimes takes ~30s on first PI confirmation when the API is cold;
      // give some headroom over the success redirect.
      await page.waitForURL(/.*\/checkout\/success\//, { timeout: 60_000 });
    });

    await test.step('Wait for the webhook to create the local subscription (via API, robust to UI polling timeouts)', async () => {
      await waitFor(
        () => listSubscriptions(user),
        (subs) => subs.length >= 1 && subs[0].status === 'ACTIVE',
        { timeoutMs: 60_000, intervalMs: 2_000, label: 'subscription created via webhook' }
      );
    });

    await test.step('Navigate to /account/subscriptions and see the active subscription', async () => {
      await page.goto('/account/subscriptions');
      await page.waitForLoadState('networkidle');

      // Wait until at least one subscription card is rendered.
      await expect(page.locator('.my-sub-card')).toHaveCount(1, { timeout: 15_000 });
      await expect(page.locator('.status-badge--active')).toBeVisible();
      await expect(page.locator('.my-sub-card__cancel')).toBeVisible();
      // Page-level header
      await expect(page.locator('.my-subs-page__header h1')).toContainText(/Mes abonnements/i);
    });

    await test.step('Customer initiates cancellation — modal appears', async () => {
      await page.locator('.my-sub-card__cancel').click();
      await expect(page.locator('.cancel-modal')).toBeVisible();
      await expect(page.locator('.cancel-modal h2')).toContainText(/Annuler/i);
    });

    await test.step('Customer confirms — subscription becomes CANCELLED', async () => {
      await page.locator('.cancel-modal .ds-btn--danger').click();
      // Modal closes
      await expect(page.locator('.cancel-modal')).not.toBeVisible({ timeout: 10_000 });
      // Status flips to CANCELLED in the UI
      await expect(page.locator('.status-badge--cancelled')).toBeVisible({ timeout: 10_000 });
      // Cancel button is no longer rendered for a cancelled sub
      await expect(page.locator('.my-sub-card__cancel')).toHaveCount(0);
    });
  });

  test('empty state when the customer has no subscriptions yet', async ({ page }) => {
    const user = await registerUser('-mysubs-empty');
    await loginViaToken(page, user);

    await page.goto('/account/subscriptions');
    await page.waitForLoadState('networkidle');

    await expect(page.locator('.my-subs-empty')).toBeVisible();
    await expect(page.locator('.my-subs-empty')).toContainText(/Aucun abonnement/i);
    // CTA back to catalog
    await expect(page.getByRole('link', { name: /Voir le catalogue/i })).toBeVisible();
  });
});
