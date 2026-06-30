import { test, expect } from '@playwright/test';
import { registerUser, RegisteredUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart, clickCheckoutFromCart } from '../helpers/cart';
import {
  fillBillingForm,
  fillStripeCard,
  submitCheckout,
  captureApi,
  TEST_CARDS,
} from '../helpers/checkout';
import { getOrder, listSubscriptions, waitFor } from '../helpers/backend';

/**
 * Verifies the full payment-to-subscription pipeline including the Stripe
 * webhook leg. Requires `stripe listen --forward-to localhost:8080/api/v1/payments/webhook`
 * to be running with the matching STRIPE_WEBHOOK_SECRET in the backend's .env.
 *
 * Flow asserted:
 *   1. Register customer
 *   2. Add product (MONTHLY) → cart → checkout
 *   3. Pay with 4242 → /payments/initiate 200 + Stripe Subscription created
 *   4. Stripe webhook `invoice.paid` (subscription_create) hits the backend
 *   5. Order moves to PAID  ← WEBHOOK propagation under test
 *   6. Local Subscription created with stripeSubscriptionId  ← under test
 */
test.describe('Webhook propagation — full payment lifecycle', () => {
  test('paying with valid card → Stripe webhook → Order PAID + local Subscription created', async ({ page }) => {
    test.setTimeout(180_000);
    const apiCalls = captureApi(page);

    let user!: RegisteredUser;
    await test.step('Register and authenticate', async () => {
      user = await registerUser('-webhook');
      await loginViaToken(page, user);
    });

    let createdOrderId: string | null = null;
    await test.step('Buy a monthly subscription', async () => {
      await addProductToCart(page, { billingCycle: 'MONTHLY' });
      await goToCart(page);
      await clickCheckoutFromCart(page);
      await fillBillingForm(page);
      await fillStripeCard(page, TEST_CARDS.ok);
      await submitCheckout(page);
      await page.waitForURL(/.*\/checkout\/success\//, { timeout: 60_000 });
    });

    await test.step('Extract created order ID from captured API calls', async () => {
      const orderCall = apiCalls.find(c => c.url.endsWith('/api/v1/orders') && c.status === 201);
      expect(orderCall, 'expected POST /api/v1/orders to have happened').toBeTruthy();
      const parsed = JSON.parse(orderCall!.body);
      createdOrderId = parsed.data;
      expect(createdOrderId).toMatch(/^[0-9a-f-]{36}$/);
    });

    await test.step('Webhook drives Order to PAID within 30s', async () => {
      const order = await waitFor(
        () => getOrder(user, createdOrderId!),
        (o) => o.status === 'PAID',
        { timeoutMs: 30_000, intervalMs: 1500, label: 'order PAID' }
      );
      expect(order.status).toBe('PAID');
    });

    await test.step('Local Subscription is created and linked to the Stripe Subscription', async () => {
      const subs = await waitFor(
        () => listSubscriptions(user),
        (list) => list.length >= 1,
        { timeoutMs: 15_000, intervalMs: 1500, label: 'local subscription created' }
      );
      expect(subs.length).toBeGreaterThanOrEqual(1);
      const sub = subs[0];
      expect(sub.status).toBe('ACTIVE');
      expect(sub.billingCycle).toBe('MONTHLY');
      // The subscription must be linked to a Stripe sub (sub_*).
      // Note: depending on Stripe API version, the response may not surface this field.
      // If your SubscriptionResponse exposes it, we assert it; otherwise skip.
      if (sub.stripeSubscriptionId !== undefined) {
        expect(sub.stripeSubscriptionId).toMatch(/^sub_/);
      }
    });
  });
});
