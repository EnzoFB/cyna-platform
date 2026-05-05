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

test.describe('Purchase — multi-product cart with same billing cycle', () => {
  test('customer can buy multiple products with the same monthly cycle', async ({ page }) => {
    test.setTimeout(120_000);
    const apiCalls = captureApi(page);

    await test.step('Register and authenticate', async () => {
      const user = await registerUser('-multi');
      await loginViaToken(page, user);
    });

    await test.step('Add three different products to the cart (all monthly)', async () => {
      await addProductToCart(page, { index: 0, billingCycle: 'MONTHLY' });
      await addProductToCart(page, { index: 1, billingCycle: 'MONTHLY' });
      await addProductToCart(page, { index: 2, billingCycle: 'MONTHLY' });
    });

    await test.step('Cart shows three lines and computes a total', async () => {
      await goToCart(page);
      await expect(page.locator('.cart-line')).toHaveCount(3);
      // Order summary total is non-empty
      await expect(page.locator('.cart-summary__total, [class*="total"]').first()).toBeVisible();
    });

    await test.step('Checkout completes with all three products in one subscription', async () => {
      await clickCheckoutFromCart(page);
      await fillBillingForm(page);
      await fillStripeCard(page, TEST_CARDS.ok);
      await submitCheckout(page);
      await page.waitForURL(/.*\/checkout\/success\//, { timeout: 60_000 });
    });

    await test.step('Single order + single subscription created (verified via API contract)', async () => {
      const orderCalls = apiCalls.filter(c => c.url.endsWith('/api/v1/orders') && c.status === 201);
      expect(orderCalls, 'exactly one order should be created').toHaveLength(1);

      const initCalls = apiCalls.filter(c => c.url.endsWith('/api/v1/payments/initiate') && c.status === 200);
      expect(initCalls, 'exactly one payment initiation').toHaveLength(1);
    });
  });
});
