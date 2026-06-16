import { expect, request, test } from '@playwright/test';
import { createAdminFixture } from '../helpers/admin';
import { API_URL, registerUser } from '../helpers/api';

function retryAfterMs(headers: Record<string, string>): number {
  const retryAfterSeconds = Number(headers['retry-after'] ?? '0');
  return Number.isFinite(retryAfterSeconds) && retryAfterSeconds > 0
    ? retryAfterSeconds * 1000
    : 0;
}

test.describe('Backoffice authentication and permissions', () => {
  test('redirects unauthenticated users to the admin login page', async ({ page }) => {
    await page.goto('/');
    await page.waitForURL(/\/login$/, { timeout: 10_000 });

    await page.goto('/users');
    await page.waitForURL(/\/login$/, { timeout: 10_000 });
  });

  test('rejects customer access on admin API and admin login form', async ({ page }) => {
    const customer = await registerUser('-bo-denied');
    const ctx = await request.newContext();

    const forbidden = await ctx.get(`${API_URL}/admin/users`, {
      headers: { Authorization: `Bearer ${customer.accessToken}` },
    });
    expect(forbidden.status()).toBe(403);

    const unauthenticated = await ctx.get(`${API_URL}/admin/users`);
    expect(unauthenticated.status()).toBe(401);
    await ctx.dispose();

    await page.goto('/login');
    await page.fill('#email', customer.email);
    await page.fill('#password', customer.password);
    let deniedLogin = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/auth/admin/login')
        && response.request().method() === 'POST',
    );
    await page.locator('button[type="submit"]').click();
    let deniedLoginResponse = await deniedLogin;

    if (deniedLoginResponse.status() === 429) {
      const retryDelayMs = retryAfterMs(deniedLoginResponse.headers());
      expect(retryDelayMs, 'rate-limited auth responses must expose Retry-After').toBeGreaterThan(0);

      deniedLogin = page.waitForResponse(
        (response) =>
          response.url().includes('/api/v1/auth/admin/login')
          && response.request().method() === 'POST',
      );
      await page.waitForTimeout(retryDelayMs);
      await page.locator('button[type="submit"]').click();
      deniedLoginResponse = await deniedLogin;
    }

    expect(deniedLoginResponse.status()).toBe(403);
    await expect(page.locator('.login-card__error')).toBeVisible();
  });

  test('authenticates an admin through credentials then OTP verification', async ({ page }) => {
    const admin = await createAdminFixture('-bo-login');

    await page.route('**/auth/login/verify-otp', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: admin.adminAccessToken,
            refreshToken: admin.adminRefreshToken,
            expiresIn: 3600,
            tokenType: 'Bearer',
          },
          timestamp: new Date().toISOString(),
        }),
      });
    });

    await page.goto('/login');
    await page.fill('#email', admin.email);
    await page.fill('#password', admin.password);

    const adminLogin = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/auth/admin/login')
        && response.request().method() === 'POST',
    );
    await page.locator('button[type="submit"]').click();
    expect((await adminLogin).status()).toBe(200);

    await expect(page.locator('#otp')).toBeVisible();
    await page.fill('#otp', '123456');
    await page.locator('button[type="submit"]').click();

    await page.waitForURL(/\/?$/, { timeout: 10_000 });
    await expect(page.locator('.shell')).toBeVisible();
    await expect(page.locator('.header__user-role')).toContainText('Admin');
  });
});
