import { test, expect } from '@playwright/test';
import { registerUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart } from '../helpers/cart';

test.describe('Cart — mixed billing cycles', () => {
  test('cart allows checkout when items mix monthly and annual cycles', async ({ page }) => {
    test.setTimeout(120_000);

    await test.step('Authenticated user', async () => {
      const user = await registerUser('-mixed');
      await loginViaToken(page, user);
    });

    await test.step('Add a MONTHLY product and an ANNUAL product to the cart', async () => {
      await addProductToCart(page, { index: 0, billingCycle: 'MONTHLY' });
      await addProductToCart(page, { index: 1, billingCycle: 'ANNUAL' });
      await goToCart(page);
    });

    await test.step('No warning banner is displayed for mixed cycles', async () => {
      await expect(page.locator('.cart-mixed-cycle-warning')).not.toBeVisible();
    });

    await test.step('Checkout button is enabled despite mixed cycles', async () => {
      const checkoutBtn = page.locator('.cart-summary__checkout');
      await expect(checkoutBtn).toBeEnabled();
    });
  });
});
