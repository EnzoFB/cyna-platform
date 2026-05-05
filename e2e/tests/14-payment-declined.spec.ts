import { test, expect } from '@playwright/test';
import { registerUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart, clickCheckoutFromCart } from '../helpers/cart';
import {
  fillBillingForm,
  fillStripeCard,
  submitCheckout,
  TEST_CARDS,
} from '../helpers/checkout';

test.describe('Payment — declined card', () => {
  test('declined card surfaces a translated error and customer can retry', async ({ page }) => {
    test.setTimeout(120_000);

    await test.step('Setup: registered customer with one product in cart on checkout page', async () => {
      const user = await registerUser('-declined');
      await loginViaToken(page, user);
      await addProductToCart(page, { billingCycle: 'MONTHLY' });
      await goToCart(page);
      await clickCheckoutFromCart(page);
      await expect(page).toHaveURL(/.*\/checkout/);
    });

    await test.step('Submit with declined card 4000 0000 0000 0002', async () => {
      await fillBillingForm(page);
      await fillStripeCard(page, TEST_CARDS.declined);
      await submitCheckout(page);
    });

    await test.step('Submit-error banner is visible with the translated decline message', async () => {
      const errorBanner = page.locator('.submit-error');
      await expect(errorBanner).toBeVisible({ timeout: 30_000 });
      await expect(errorBanner).toContainText(/refus|declin/i);
    });

    await test.step('User stays on checkout (no redirect to /catalog) and cart is preserved', async () => {
      await expect(page).toHaveURL(/.*\/checkout/);
      // Submit button is re-enabled so the user can correct and retry.
      await expect(page.locator('.checkout-submit')).toBeEnabled();
    });
  });
});
