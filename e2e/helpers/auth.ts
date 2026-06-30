import { Page } from '@playwright/test';
import { RegisteredUser } from './api';

function retryAfterMs(headers: Record<string, string>): number {
  const retryAfterSeconds = Number(headers['retry-after'] ?? '0');
  return Number.isFinite(retryAfterSeconds) && retryAfterSeconds > 0
    ? retryAfterSeconds * 1000
    : 0;
}

/**
 * "Logs in" the test user by writing the registration-issued refresh token
 * directly into the browser as an HttpOnly cookie matching the one the
 * backend sets on /auth/login/verify-otp. The PWA's APP_INITIALIZER runs
 * restoreSession() on the next page.goto, which exchanges this cookie
 * for a fresh access token and authenticates the user before any feature
 * component fires HTTP.
 *
 * The UI login (credentials → OTP) is bypassed deliberately: the raw OTP
 * is hashed in the DB (HMAC pepper) and unreadable from the test. The
 * /auth/register helper returns a refresh token the backend treats the
 * same as one issued via verify-otp.
 */
export async function loginViaToken(page: Page, user: RegisteredUser): Promise<void> {
  await page.context().addCookies([
    {
      name: 'refresh_token',
      value: user.refreshToken,
      domain: 'localhost',
      path: '/api/v1/auth',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);
  let bootstrapRefresh = page.waitForResponse(
    (response) =>
      response.url().includes('/api/v1/auth/refresh')
      && response.request().method() === 'POST',
  );

  // Navigate so APP_INITIALIZER triggers the bootstrap refresh against
  // the cookie we just placed. baseURL comes from playwright.config.
  await page.goto('/');
  let refreshResponse = await bootstrapRefresh;

  if (refreshResponse.status() === 429) {
    const retryDelayMs = retryAfterMs(refreshResponse.headers());

    if (retryDelayMs <= 0) {
      throw new Error('Bootstrap refresh was rate-limited without Retry-After header');
    }

    bootstrapRefresh = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/auth/refresh')
        && response.request().method() === 'POST',
    );
    await page.waitForTimeout(retryDelayMs);
    await page.reload();
    refreshResponse = await bootstrapRefresh;
  }

  if (!refreshResponse.ok()) {
    throw new Error(`Bootstrap refresh failed with status ${refreshResponse.status()}`);
  }

  await page.waitForLoadState('networkidle');
}
