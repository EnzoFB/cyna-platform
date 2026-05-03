import { request } from '@playwright/test';
import { API_URL, RegisteredUser } from './api';

export interface OrderApiResponse {
  id: string;
  status: string;
  totalAmount: number;
  currency: string;
}

export interface SubscriptionApiResponse {
  id: string;
  productName: string;
  status: string;
  billingCycle: string;
  stripeSubscriptionId?: string;
}

export async function getOrder(user: RegisteredUser, orderId: string): Promise<OrderApiResponse> {
  const ctx = await request.newContext();
  const res = await ctx.get(`${API_URL}/orders/${orderId}`, {
    headers: { Authorization: `Bearer ${user.accessToken}` },
  });
  if (!res.ok()) {
    await ctx.dispose();
    throw new Error(`getOrder failed: ${res.status()} ${await res.text()}`);
  }
  const body = await res.json();
  await ctx.dispose();
  return body.data;
}

export async function listSubscriptions(user: RegisteredUser): Promise<SubscriptionApiResponse[]> {
  const ctx = await request.newContext();
  const res = await ctx.get(`${API_URL}/subscriptions?page=0&size=20`, {
    headers: { Authorization: `Bearer ${user.accessToken}` },
  });
  if (!res.ok()) {
    await ctx.dispose();
    throw new Error(`listSubscriptions failed: ${res.status()} ${await res.text()}`);
  }
  const body = await res.json();
  await ctx.dispose();
  return body.data.items;
}

/**
 * Polls the backend until the predicate becomes truthy or the deadline expires.
 * Used to wait for webhook-driven state changes (Order PAID, Subscription created).
 */
export async function waitFor<T>(
  fetcher: () => Promise<T>,
  predicate: (value: T) => boolean,
  options: { timeoutMs?: number; intervalMs?: number; label?: string } = {}
): Promise<T> {
  const timeoutMs = options.timeoutMs ?? 20_000;
  const intervalMs = options.intervalMs ?? 1_000;
  const deadline = Date.now() + timeoutMs;
  let lastValue: T | undefined;

  while (Date.now() < deadline) {
    try {
      lastValue = await fetcher();
      if (predicate(lastValue)) return lastValue;
    } catch {
      // ignore transient errors during polling
    }
    await new Promise((r) => setTimeout(r, intervalMs));
  }

  throw new Error(
    `waitFor timed out${options.label ? ` (${options.label})` : ''}; last value: ${JSON.stringify(lastValue)}`
  );
}
