import { test, expect } from '@playwright/test';
import { registerUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart, clickCheckoutFromCart } from '../helpers/cart';
import {
  fillBillingForm,
  fillStripeCard,
  submitCheckout,
  captureApi,
  TEST_CARDS,
} from '../helpers/checkout';

test.describe('Purchase — monthly subscription', () => {
  test('customer can buy a monthly subscription end-to-end', async ({ page }) => {
    test.setTimeout(120_000);
    const apiCalls = captureApi(page);

    await test.step('Register and authenticate', async () => {
      const user = await registerUser('-monthly');
      await loginViaToken(page, user);
    });

    await test.step('Browse catalog and add a product (MONTHLY) to the cart', async () => {
      await addProductToCart(page, { billingCycle: 'MONTHLY' });
    });

    await test.step('Cart shows the item with monthly billing', async () => {
      await goToCart(page);
      await expect(page.locator('.cart-line')).toHaveCount(1);
      // Default billing cycle on a new line is monthly.
      const select = page.locator('.cart-line__cycle select').first();
      await expect(select).toHaveValue('MONTHLY');
    });

    await test.step('Proceed to checkout — recurring notice + correct submit label', async () => {
      await clickCheckoutFromCart(page);
      await expect(page).toHaveURL(/.*\/checkout/);
      // Recurring notice visible
      await expect(page.locator('.recurring-notice')).toBeVisible();
      await expect(page.locator('.recurring-notice')).toContainText(/mois/);
      // Submit button reflects monthly billing
      await expect(page.locator('.checkout-submit')).toContainText(/abonner.*mensuel/i);
    });

    await test.step('Fill billing + payment forms', async () => {
      await fillBillingForm(page);
      await fillStripeCard(page, TEST_CARDS.ok);
    });

    await test.step('Submit lands on the order-confirmation page', async () => {
      await submitCheckout(page);
      await page.waitForURL(/.*\/checkout\/success\//, { timeout: 60_000 });

      // Cart counter should now be empty.
      const cartLink = page.getByRole('link', { name: /panier/i }).first();
      await expect(cartLink).toContainText('(0)');
    });

    await test.step('Confirmation page shows order details + activated services', async () => {
      // Either "Paiement validé !" (webhook fired) or "Paiement en cours de validation"
      const heading = page.locator('.confirmation-card h1');
      await expect(heading).toBeVisible({ timeout: 15_000 });
      await expect(heading).toContainText(/Paiement/);

      // Order summary is rendered with at least one activated service.
      await expect(page.locator('.confirmation-line')).toHaveCount(1);
      // CTA back to catalog is present
      await expect(page.getByRole('link', { name: /Continuer mes achats/i })).toBeVisible();
    });

    await test.step('Backend contract is correct: order created, subscription initiated', async () => {
      const orderCall = apiCalls.find(c => c.url.endsWith('/api/v1/orders') && c.status === 201);
      expect(orderCall, 'POST /api/v1/orders should return 201').toBeTruthy();

      const initCall = apiCalls.find(c => c.url.endsWith('/api/v1/payments/initiate') && c.status === 200);
      expect(initCall, 'POST /api/v1/payments/initiate should return 200').toBeTruthy();
      expect(initCall!.body).toMatch(/clientSecret/);
      expect(initCall!.body).toMatch(/pi_/); // Stripe PaymentIntent id present in clientSecret
    });
  });
});
