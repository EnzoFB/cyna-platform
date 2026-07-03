import { request, APIRequestContext, APIResponse } from '@playwright/test';
import { activateUserAndIssueRefreshToken } from './db';

export const API_URL = process.env.API_URL ?? 'http://localhost:8080/api/v1';
const RATE_LIMIT_STATUS = 429;
const MAX_RATE_LIMIT_RETRIES = 3;

export interface RegisteredUser {
  email: string;
  password: string;
  accessToken: string;
  refreshToken: string;
}

export interface RefreshedTokens {
  accessToken: string;
  refreshToken: string;
}

function resolveRetryDelayMs(response: APIResponse): number {
  const retryAfterSeconds = Number(response.headers()['retry-after'] ?? '0');
  return Number.isFinite(retryAfterSeconds) && retryAfterSeconds > 0
    ? retryAfterSeconds * 1000
    : 0;
}

async function postWithRateLimitRetry(
  ctx: APIRequestContext,
  url: string,
  data: Record<string, unknown>,
): Promise<APIResponse> {
  for (let attempt = 1; attempt <= MAX_RATE_LIMIT_RETRIES; attempt += 1) {
    const response = await ctx.post(url, { data });

    if (response.status() !== RATE_LIMIT_STATUS) {
      return response;
    }

    const retryDelayMs = resolveRetryDelayMs(response);
    if (attempt === MAX_RATE_LIMIT_RETRIES || retryDelayMs <= 0) {
      return response;
    }

    await new Promise((resolve) => setTimeout(resolve, retryDelayMs));
  }

  throw new Error(`postWithRateLimitRetry exhausted retries for ${url}`);
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

  const res = await postWithRateLimitRetry(ctx, `${API_URL}/auth/register`, {
    email,
    password,
    firstName: 'PW',
    lastName: 'Test',
    company: 'CYNA E2E',
    lang: 'fr',
    acceptTerms: true,
  });

  if (!res.ok()) {
    throw new Error(`registerUser failed: ${res.status()} ${await res.text()}`);
  }

  const body = await res.json();
  await ctx.dispose();

  const accessToken = body?.data?.accessToken;
  const refreshToken = body?.data?.refreshToken;

  if (typeof accessToken === 'string' && typeof refreshToken === 'string') {
    return {
      email,
      password,
      accessToken,
      refreshToken,
    };
  }

  const bootstrapRefreshToken = await activateUserAndIssueRefreshToken(email);
  const bootstrappedTokens = await refreshSessionTokens(bootstrapRefreshToken);

  return {
    email,
    password,
    accessToken: bootstrappedTokens.accessToken,
    refreshToken: bootstrappedTokens.refreshToken,
  };
}

export async function refreshSessionTokens(refreshToken: string): Promise<RefreshedTokens> {
  const ctx: APIRequestContext = await request.newContext();

  const csrfRes = await ctx.get(`${API_URL}/auth/csrf`);
  if (!csrfRes.ok()) {
    throw new Error(`refreshSessionTokens: CSRF fetch failed: ${csrfRes.status()} ${await csrfRes.text()}`);
  }
  const csrfBody = await csrfRes.json();
  const csrfToken: string = csrfBody.data.token;
  const csrfHeader: string = csrfBody.data.headerName ?? 'X-XSRF-TOKEN';

  const res = await ctx.post(`${API_URL}/auth/refresh`, {
    data: { refreshToken },
    headers: { [csrfHeader]: csrfToken },
  });

  if (!res.ok()) {
    throw new Error(`refreshSessionTokens failed: ${res.status()} ${await res.text()}`);
  }

  const body = await res.json();
  await ctx.dispose();

  return {
    accessToken: body.data.accessToken,
    refreshToken: body.data.refreshToken,
  };
}
