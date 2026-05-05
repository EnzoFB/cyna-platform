import { Page } from '@playwright/test';
import { RegisteredUser } from './api';

/**
 * "Logs in" the test user by writing the refreshToken to the PWA's localStorage.
 * The AuthService restores the session lazily — the auth guard on /checkout
 * triggers /api/v1/auth/refresh when needed. We don't pre-emptively refresh
 * here so this helper stays fast and free of timing assumptions.
 */
export async function loginViaToken(page: Page, user: RegisteredUser): Promise<void> {
  // Navigate to the home page so we have a same-origin context for localStorage.
  await page.goto('/');
  await page.evaluate((token) => {
    localStorage.setItem('refreshToken', token);
  }, user.refreshToken);
}
