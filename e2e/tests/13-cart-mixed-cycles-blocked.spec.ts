import { test, expect } from '@playwright/test';
import { registerUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart } from '../helpers/cart';

test.describe('Cart — mixed billing cycles', () => {
  test('cart blocks checkout when items mix monthly and annual cycles', async ({ page }) => {
    test.setTimeout(120_000);

    await test.step('Authenticated user', async () => {
      const user = await registerUser('-mixed');
      await loginViaToken(page, user);
    });

    await test.step('Add a MONTHLY product and a different ANNUAL product to the cart', async () => {
      await addProductToCart(page, { index: 0, billingCycle: 'MONTHLY' });
      await addProductToCart(page, { index: 1, billingCycle: 'MONTHLY' });
      // Switch the second one to ANNUAL via the cart UI
      await goToCart(page);
      const cycleSelects = page.locator('.cart-line__cycle select');
      await expect(cycleSelects).toHaveCount(2);
      await cycleSelects.nth(1).selectOption('ANNUAL');
    });

    await test.step('Cart displays the mixed-cycles warning banner', async () => {
      await expect(page.locator('.cart-mixed-cycle-warning')).toBeVisible();
      await expect(page.locator('.cart-mixed-cycle-warning')).toContainText(/différents|differents|different/i);
    });

    await test.step('Checkout button is disabled', async () => {
      const checkoutBtn = page.locator('.cart-summary__checkout');
      await expect(checkoutBtn).toBeDisabled();
    });

    await test.step('User can recover by aligning cycles', async () => {
      // Set both back to MONTHLY
      const cycleSelects = page.locator('.cart-line__cycle select');
      await cycleSelects.nth(1).selectOption('MONTHLY');
      await expect(page.locator('.cart-mixed-cycle-warning')).not.toBeVisible();
      await expect(page.locator('.cart-summary__checkout')).toBeEnabled();
    });
  });
});
