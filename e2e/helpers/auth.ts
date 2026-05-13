import { Page } from '@playwright/test';
import { RegisteredUser } from './api';

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
  // Navigate so APP_INITIALIZER triggers the bootstrap refresh against
  // the cookie we just placed. baseURL comes from playwright.config.
  await page.goto('/');
}
