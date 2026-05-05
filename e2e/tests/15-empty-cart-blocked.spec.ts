import { test, expect } from '@playwright/test';
import { registerUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { goToCart } from '../helpers/cart';

test.describe('Cart — empty cart guards', () => {
  test('an empty cart shows a friendly empty state and no checkout button', async ({ page }) => {
    test.setTimeout(60_000);

    const user = await registerUser('-empty');
    await loginViaToken(page, user);

    await goToCart(page);
    // Empty-state container is rendered when cart is empty.
    await expect(page.locator('.cart-empty')).toBeVisible();
    await expect(page.locator('.cart-empty')).toContainText(/vide|empty/i);
    // The order summary with the checkout button is not rendered for an empty cart.
    await expect(page.locator('.cart-summary__checkout')).toHaveCount(0);
  });
});
