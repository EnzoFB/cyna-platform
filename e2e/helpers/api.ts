import { request, APIRequestContext } from '@playwright/test';

export const API_URL = 'http://localhost:8080/api/v1';

export interface RegisteredUser {
  email: string;
  password: string;
  accessToken: string;
  refreshToken: string;
}

/**
 * Registers a fresh customer via the backend API and returns the credentials
 * + tokens. Bypasses the UI for fast and reliable test setup.
 */
export async function registerUser(suffix = ''): Promise<RegisteredUser> {
  const ctx: APIRequestContext = await request.newContext();
  const stamp = Date.now() + Math.floor(Math.random() * 10_000);
  const email = `pw-${stamp}${suffix}@cyna-test.local`;
  const password = 'TestPwd!2026';

  const res = await ctx.post(`${API_URL}/auth/register`, {
    data: {
      email,
      password,
      firstName: 'PW',
      lastName: 'Test',
      lang: 'fr',
    },
  });

  if (!res.ok()) {
    throw new Error(`registerUser failed: ${res.status()} ${await res.text()}`);
  }

  const body = await res.json();
  await ctx.dispose();

  return {
    email,
    password,
    accessToken: body.data.accessToken,
    refreshToken: body.data.refreshToken,
  };
}
