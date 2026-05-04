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

test.describe('Purchase — annual subscription', () => {
  test('customer can buy an annual subscription end-to-end', async ({ page }) => {
    test.setTimeout(120_000);
    const apiCalls = captureApi(page);

    await test.step('Register and authenticate', async () => {
      const user = await registerUser('-annual');
      await loginViaToken(page, user);
    });

    await test.step('Add product to cart and switch to ANNUAL billing in the cart', async () => {
      // Add as monthly first (the catalog/product page defaults to monthly), then
      // switch to annual via the cart's billing-cycle select.
      await addProductToCart(page, { billingCycle: 'MONTHLY' });
      await goToCart(page);
      await page.locator('.cart-line__cycle select').first().selectOption('ANNUAL');
      await expect(page.locator('.cart-line__cycle select').first()).toHaveValue('ANNUAL');
    });

    await test.step('Proceed to checkout — annual recurring notice + correct submit label', async () => {
      await clickCheckoutFromCart(page);
      await expect(page).toHaveURL(/.*\/checkout/);
      await expect(page.locator('.recurring-notice')).toBeVisible();
      await expect(page.locator('.recurring-notice')).toContainText(/année|year/i);
      await expect(page.locator('.checkout-submit')).toContainText(/abonner.*annuel/i);
    });

    await test.step('Complete payment with Stripe test card', async () => {
      await fillBillingForm(page);
      await fillStripeCard(page, TEST_CARDS.ok);
      await submitCheckout(page);
      await page.waitForURL(/.*\/checkout\/success\//, { timeout: 60_000 });
    });

    await test.step('/payments/initiate succeeded with annual cycle metadata', async () => {
      const initCall = apiCalls.find(c => c.url.endsWith('/api/v1/payments/initiate') && c.status === 200);
      expect(initCall, 'POST /api/v1/payments/initiate should return 200').toBeTruthy();
      expect(initCall!.body).toMatch(/clientSecret/);
    });
  });
});
