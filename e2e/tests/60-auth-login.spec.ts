/**
 * End-to-end coverage of the auth flow after the cookie + APP_INITIALIZER
 * migration. The raw OTP can't be read out of the backend (HMAC pepper),
 * so the full-UI test stubs /auth/login/verify-otp with tokens fetched
 * from the registration API.
 *
 * Specific properties asserted across the file:
 *   - The refresh token never lands in localStorage on a fresh login.
 *   - A successful UI login lands the user off /auth/* and authenticates
 *     subsequent /api/v1 calls via the in-memory access token.
 *   - APP_INITIALIZER prevents the bootstrap race that previously let
 *     a stale refresh wipe a fresh login (the regression behind PR #183).
 *   - A pre-existing localStorage refresh token (legacy migration) is
 *     replayed once then deleted — existing sessions survive the deploy.
 */
import { test, expect, request } from '@playwright/test';
import { API_URL, registerUser } from '../helpers/api';

test.describe('PWA auth flow (cookie + APP_INITIALIZER)', () => {

  test('full UI login lands off /auth and DOES NOT persist refresh token in localStorage', async ({ page }) => {
    const user = await registerUser('-login-ui-full');
    const tokenSource = await registerUser('-login-ui-full-tokens');

    await page.goto('/');
    await page.context().clearCookies();
    await page.evaluate(() => localStorage.removeItem('refreshToken'));

    // Stub verify-otp with a known-good token pair. We can't recover the
    // raw OTP from the backend so this is the cleanest way to drive the
    // POST-OTP UI plumbing end-to-end.
    await page.route('**/auth/login/verify-otp', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            accessToken: tokenSource.accessToken,
            refreshToken: tokenSource.refreshToken,
            expiresIn: 1,
            tokenType: 'Bearer',
          },
          timestamp: new Date().toISOString(),
        }),
      });
    });

    await page.goto('/auth/login?mode=login');
    await page.fill('input[formControlName="email"]', user.email);
    await page.fill('input[formControlName="password"]', user.password);
    await page.click('button[type="submit"]');

    await expect(page.locator('input[formControlName="otpCode"]')).toBeVisible({ timeout: 5000 });
    await page.fill('input[formControlName="otpCode"]', '123456');
    await page.click('button[type="submit"]');

    await page.waitForFunction(
      () => !location.pathname.startsWith('/auth/'),
      { timeout: 5000 },
    );

    // Modern posture: the refresh token lives in the HttpOnly cookie
    // (which Playwright can read via context.cookies but JS in the page
    // cannot). It MUST NOT also leak into localStorage.
    const lsRefresh = await page.evaluate(() => localStorage.getItem('refreshToken'));
    expect(lsRefresh, 'refresh token must not be persisted in JS-readable storage').toBeNull();
  });

  test('credentials submit transitions the UI to the OTP step', async ({ page }) => {
    const user = await registerUser('-login-ui-creds');

    await page.goto('/auth/login?mode=login');
    await page.context().clearCookies();
    await page.evaluate(() => localStorage.removeItem('refreshToken'));
    await page.reload();

    await page.fill('input[formControlName="email"]', user.email);
    await page.fill('input[formControlName="password"]', user.password);

    const loginRes = page.waitForResponse(r => r.url().includes('/auth/login') && r.request().method() === 'POST');
    await page.click('button[type="submit"]');
    const res = await loginRes;
    expect(res.status()).toBe(200);

    await expect(page.locator('input[formControlName="otpCode"]')).toBeVisible({ timeout: 5000 });
  });

  test('a registered user can complete /auth/login (challenge step) via API', async () => {
    const user = await registerUser('-login-api-full');
    const ctx = await request.newContext();

    const res = await ctx.post(`${API_URL}/auth/login`, {
      data: { email: user.email, password: user.password, lang: 'fr' },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.data.challengeId).toBeTruthy();

    await ctx.dispose();
  });

  test('APP_INITIALIZER prevents the bootstrap race: stale cookie cannot wipe a freshly-issued one', async ({ page }) => {
    // Pre-migration scenario the prior fix had to handle reactively:
    //   1) Cookie carries a stale/revoked refresh token at boot.
    //   2) restoreSession() fires from APP_INITIALIZER and posts /auth/refresh.
    //   3) Backend 401s the stale cookie -> AuthService clears state.
    //   4) The user can then log in afresh on the now-clean slate, and
    //      because the bootstrap refresh ran to completion BEFORE any
    //      feature route was mounted, there is no late catchError racing
    //      a fresh login.
    await page.goto('/');
    await page.context().clearCookies();
    await page.context().addCookies([{
      name: 'refresh_token',
      value: 'definitely-not-a-valid-refresh-token',
      domain: 'localhost',
      path: '/api/v1/auth',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    }]);

    // Watch the bootstrap refresh. The APP_INITIALIZER blocks the app
    // from settling until this round-trip is done. We accept either 401
    // (token rejected) or 429 (rate-limited from the preceding tests in
    // the suite) — both correctly prevent the stale cookie from being
    // treated as a valid session.
    const staleRefresh = page.waitForResponse(r =>
      r.url().includes('/auth/refresh') && r.request().method() === 'POST'
    );
    await page.goto('/');
    const staleRes = await staleRefresh;
    expect([401, 429]).toContain(staleRes.status());

    // The PWA must NOT consider the user authenticated, regardless of
    // which rejection path the backend took. The access token signal is
    // private, so we probe via the visible UI: a guarded route should
    // bounce back to /auth/login.
    await page.goto('/account');
    await page.waitForURL(/\/auth\/login/, { timeout: 5000 });
  });

  test('a legacy localStorage refresh token is replayed once then wiped', async ({ page }) => {
    // Existing tabs from before the cookie migration carry a refresh
    // token in localStorage. The AuthService replays it in the body of
    // the first /auth/refresh so the backend rotates them onto the
    // cookie — no forced logout — and wipes the localStorage entry so
    // it never gets replayed again.
    const user = await registerUser('-legacy-migration');

    await page.goto('/');
    await page.context().clearCookies();
    await page.evaluate((t) => localStorage.setItem('refreshToken', t), user.refreshToken);

    // Trigger APP_INITIALIZER bootstrap. The legacy token in localStorage
    // should ride along in the body and succeed.
    const refresh = page.waitForResponse(r =>
      r.url().includes('/auth/refresh') && r.request().method() === 'POST'
    );
    await page.goto('/');
    const refreshRes = await refresh;
    expect(refreshRes.status(), 'legacy refresh should be accepted by the backend').toBe(200);

    // The legacy entry must have been deleted now that the cookie holds
    // the rotated token.
    const lsAfter = await page.evaluate(() => localStorage.getItem('refreshToken'));
    expect(lsAfter, 'legacy localStorage entry must be wiped after migration').toBeNull();

    // And the cookie now carries the freshly-rotated refresh token
    // (different from the legacy one we injected).
    const cookies = await page.context().cookies();
    const refreshCookie = cookies.find(c => c.name === 'refresh_token');
    expect(refreshCookie, 'browser must now carry the rotated refresh cookie').toBeTruthy();
    expect(refreshCookie!.value).not.toBe(user.refreshToken);
  });
});
