import { test, expect } from '@playwright/test';
import { registerUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart, clickCheckoutFromCart } from '../helpers/cart';
import {
  fillBillingForm,
  fillStripeCard,
  submitCheckout,
} from '../helpers/checkout';

/**
 * Verifies the 3D Secure / SCA path required for many European cards.
 *
 * Stripe test card `4000 0025 0000 3155`:
 *   - Always triggers 3D Secure 2 authentication
 *   - The challenge is displayed inside a Stripe-hosted iframe with a
 *     `Complete authentication` button
 *
 * If the user clicks "Complete", the PaymentIntent moves to `succeeded`
 * and the rest of the flow continues exactly like the happy path.
 */
const CARD_3DS_REQUIRED = { number: '4000 0025 0000 3155', expiry: '12 / 30', cvc: '123' };

test.describe('Payment — 3D Secure authentication', () => {
  test('customer completes 3DS challenge then payment succeeds', async ({ page }) => {
    test.setTimeout(180_000);

    await test.step('Setup: customer reaches the checkout page', async () => {
      const user = await registerUser('-3ds');
      await loginViaToken(page, user);
      await addProductToCart(page, { billingCycle: 'MONTHLY' });
      await goToCart(page);
      await clickCheckoutFromCart(page);
    });

    await test.step('Submit billing + 3DS-requiring card', async () => {
      await fillBillingForm(page);
      await fillStripeCard(page, CARD_3DS_REQUIRED);
      await submitCheckout(page);
    });

    await test.step('Customer completes the 3D Secure challenge', async () => {
      // The challenge UI lives in a Stripe-hosted iframe (potentially nested).
      // We poll all frames in the page until one of them exposes the COMPLETE
      // button — this is robust to Stripe's frame-naming changes.
      await expect.poll(async () => {
        for (const frame of page.frames()) {
          const btn = frame.locator('button:has-text("COMPLETE")').first();
          if (await btn.count() > 0 && await btn.isVisible().catch(() => false)) {
            return frame.url();
          }
        }
        return null;
      }, { timeout: 60_000, message: 'Stripe 3DS COMPLETE button never appeared' })
        .not.toBeNull();

      // Click the button on whichever frame has it.
      for (const frame of page.frames()) {
        const btn = frame.locator('button:has-text("COMPLETE")').first();
        if (await btn.count() > 0 && await btn.isVisible().catch(() => false)) {
          await btn.click();
          break;
        }
      }
    });

    await test.step('Payment succeeds and we land on the order confirmation page', async () => {
      await page.waitForURL(/.*\/checkout\/success\//, { timeout: 60_000 });
      await expect(page.locator('.confirmation-card')).toBeVisible();
    });
  });
});
