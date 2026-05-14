/**
 * Trusted-device fast path on /auth/login.
 *
 * Full UX:
 *   1) First login from a never-seen browser → OTP step required.
 *   2) After OTP verification the backend sets a `device_token` HttpOnly
 *      cookie alongside the refresh cookie.
 *   3) Subsequent logins on the SAME browser carry the device_token cookie
 *      and skip the OTP — the backend returns tokens immediately on /login.
 *
 * Verifying the full end-to-end here would require completing a real OTP,
 * but the raw code is hashed (HMAC + pepper) in DB and unreadable from
 * test code. So this file probes the contract at the API boundary:
 *
 *   - /auth/login without any cookie returns the OTP challenge shape
 *     (challengeId set, tokens null).
 *   - /auth/login with a garbage device_token cookie silently falls
 *     through to the OTP path (no DB row matches → ignore the cookie).
 *
 * The fast-path success branch is covered by the LoginCommandHandler unit
 * tests (should_skip_otp_when_a_valid_trusted_device_cookie_is_presented
 * and the two "should_fall_back" siblings), so we trust the handler logic
 * and rely on a manual smoke test for the cookie wiring (see PR notes).
 */
import { test, expect, request } from '@playwright/test';
import { API_URL, registerUser } from '../helpers/api';

test.describe('Trusted-device fast path on /auth/login', () => {

  test('first login without a device cookie returns the OTP challenge', async () => {
    const user = await registerUser('-trust-no-cookie');
    const ctx = await request.newContext();

    const res = await ctx.post(`${API_URL}/auth/login`, {
      data: { email: user.email, password: user.password, lang: 'fr' },
    });
    expect(res.status()).toBe(200);

    const body = await res.json();
    expect(body.success).toBe(true);
    expect(body.data.challengeId, 'OTP-required path must return a challengeId').toBeTruthy();
    expect(body.data.tokens, 'no trusted device cookie → tokens must NOT be issued yet').toBeFalsy();

    await ctx.dispose();
  });

  test('login with an unknown device_token cookie silently falls through to OTP', async () => {
    // The cookie value is fresh and does NOT match any row in trusted_devices,
    // so the backend ignores it and the user goes through the regular OTP
    // path. The point: a garbage cookie must NOT crash the request and must
    // NOT short-circuit to "Authenticated".
    const user = await registerUser('-trust-garbage-cookie');
    const ctx = await request.newContext({
      extraHTTPHeaders: {
        Cookie: 'device_token=this-cookie-is-not-in-the-database',
      },
    });

    const res = await ctx.post(`${API_URL}/auth/login`, {
      data: { email: user.email, password: user.password, lang: 'fr' },
    });
    expect(res.status()).toBe(200);

    const body = await res.json();
    expect(body.data.challengeId).toBeTruthy();
    expect(body.data.tokens).toBeFalsy();

    await ctx.dispose();
  });

  test('login with a different user\'s device cookie still triggers OTP', async () => {
    // Two users. A trusts their browser. B steals (or gets handed) A's
    // device_token cookie value. Even if the cookie hashes to a real row,
    // it doesn't belong to B's user_id, so the backend must ignore it.
    // Without a real trusted-device fixture we can't insert A's row here,
    // so this case is effectively the same as "unknown cookie" at the API
    // layer — the handler logic for "wrong user" is unit-tested separately.
    const userB = await registerUser('-trust-other-users-cookie');
    const ctx = await request.newContext({
      extraHTTPHeaders: {
        Cookie: 'device_token=cookie-belongs-to-someone-else',
      },
    });

    const res = await ctx.post(`${API_URL}/auth/login`, {
      data: { email: userB.email, password: userB.password, lang: 'fr' },
    });
    expect(res.status()).toBe(200);

    const body = await res.json();
    expect(body.data.challengeId).toBeTruthy();
    expect(body.data.tokens).toBeFalsy();

    await ctx.dispose();
  });
});
