import { expect, test } from '@playwright/test';
import { loginViaToken } from '../helpers/auth';
import { registerUser } from '../helpers/api';

test.describe('PWA mobile smoke', () => {
  test('supports burger navigation, mobile filters and authenticated account access', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('.app-header')).toBeVisible();
    await page.locator('button.burger').click();
    await expect(page.locator('.user-menu')).toBeVisible();
    await page.locator('.user-menu a[href="/contact"]').click();
    await expect(page).toHaveURL(/\/contact$/);

    await page.goto('/catalog');
    await page.locator('.catalog-filters-toggle').click();
    await expect(page.locator('.catalog-filters')).toHaveClass(/is-open/);

    const user = await registerUser('-mobile');
    await loginViaToken(page, user);
    await page.goto('/account');

    await expect(page.locator('.account-page')).toBeVisible();
    await page.locator('.account-tabs button').nth(4).click();
    await expect(page.locator('.portal-banner__cta')).toBeVisible();
  });
});
