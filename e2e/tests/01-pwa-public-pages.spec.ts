import { expect, test } from '@playwright/test';

test.describe('PWA public routes and guards', () => {
  test('loads the application shell and main public pages', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('.app-header')).toBeVisible();
    await expect(page.locator('.home-content__hero')).toBeVisible();

    const navLinks = page.locator('.header-bottom .header-nav-link');
    await expect(navLinks).toHaveCount(3);

    await navLinks.nth(1).click();
    await expect(page).toHaveURL(/\/offers(?:\?.*)?$/);
    await expect(page.locator('.offers-page')).toBeVisible();

    await navLinks.nth(2).click();
    await expect(page).toHaveURL(/\/catalog(?:\?.*)?$/);
    await expect(page.locator('.catalog-page')).toBeVisible();

    const publicRoutes = [
      { path: '/about', selector: '.about-container h1' },
      { path: '/legal-notice', selector: '.legal-notice-container h1' },
      { path: '/terms', selector: '.terms-container h1' },
      { path: '/privacy-policy', selector: '.privacy-container h1' },
      { path: '/contact', selector: '.contact-container h1' },
    ];

    for (const route of publicRoutes) {
      await page.goto(route.path);
      await expect(page).toHaveURL(new RegExp(`${route.path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?:\\?.*)?$`));
      await expect(page.locator(route.selector)).toBeVisible();
    }
  });

  test('shows login and register entries in the burger menu', async ({ page }) => {
    await page.goto('/');

    await page.locator('button.burger').click();
    const menu = page.locator('.user-menu');

    await expect(menu).toBeVisible();
    await expect(menu.locator('a[href*="/auth"][href*="mode=login"]')).toBeVisible();
    await expect(menu.locator('a[href*="/auth"][href*="mode=register"]')).toBeVisible();
  });

  test('redirects unauthenticated visitors away from protected pages', async ({ page }) => {
    await page.goto('/account');
    await page.waitForURL(/\/auth\/login(?:\?.*)?$/, { timeout: 10_000 });

    await page.goto('/checkout');
    await page.waitForURL(/\/auth\/login(?:\?.*)?$/, { timeout: 10_000 });
  });
});
