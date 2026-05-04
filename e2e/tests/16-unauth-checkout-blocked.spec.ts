import { test, expect } from '@playwright/test';

test.describe('Auth — checkout requires authentication', () => {
  test('an unauthenticated user trying to reach /checkout is redirected to login', async ({ page }) => {
    test.setTimeout(30_000);

    // Make sure no leftover auth from a previous test
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());

    await page.goto('/checkout');
    // The auth guard redirects to /auth/login.
    await page.waitForURL(/.*\/auth\/login/, { timeout: 10_000 });
    await expect(page).toHaveURL(/.*\/auth\/login/);
  });
});
