import { test, expect } from '@playwright/test';
import { registerUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';
import { addProductToCart, goToCart, clickCheckoutFromCart } from '../helpers/cart';
import {
  fillBillingForm,
  fillStripeCard,
  submitCheckout,
  readSummaryAmounts,
  captureRequests,
  TEST_CARDS,
  VALID_FR_ADDRESS,
} from '../helpers/checkout';

/**
 * VAT user-journey coverage. We test what is deterministically ours:
 *  1. the order-summary VAT *estimate* (20% of HT, TTC = HT + VAT) + the
 *     "estimated VAT" note shown at checkout, and
 *  2. that the B2B VAT number typed at checkout is actually SENT to
 *     /payments/finalize (the input that lets the backend trigger the reverse
 *     charge). The *applied* VAT is Stripe's job and is verified on the invoice.
 */
test.describe('Checkout — VAT', () => {
  test('order summary shows a 20% VAT estimate that adds up, plus the estimate note', async ({ page }) => {
    test.setTimeout(60_000);

    const user = await registerUser('-vat-display');
    await loginViaToken(page, user);

    await addProductToCart(page, { billingCycle: 'MONTHLY' });
    await goToCart(page);
    await clickCheckoutFromCart(page);
    await expect(page).toHaveURL(/.*\/checkout/);

    const { subtotalHt, vat, totalTtc } = await readSummaryAmounts(page);

    // Subtotal must be a real amount, not zero.
    expect(subtotalHt).toBeGreaterThan(0);
    // VAT estimate is 20% of the HT subtotal (±1 to absorb integer rounding).
    expect(vat).toBeCloseTo(subtotalHt * 0.2, 0);
    // The three displayed figures add up: HT + VAT = TTC (±0.01 for float rounding).
    expect(totalTtc).toBeCloseTo(subtotalHt + vat, 2);

    // At checkout, the "VAT is indicative / final amount on the invoice" note is shown.
    await expect(page.locator('.cart-summary__vat-note')).toBeVisible();
  });

  test('the B2B VAT number entered at checkout is forwarded to /payments/finalize', async ({ page }) => {
    test.setTimeout(120_000);
    const requests = captureRequests(page);

    const user = await registerUser('-vat-finalize');
    await loginViaToken(page, user);

    await addProductToCart(page, { billingCycle: 'MONTHLY' });
    await goToCart(page);
    await clickCheckoutFromCart(page);
    await expect(page).toHaveURL(/.*\/checkout/);

    // Fill the billing form WITH a VAT number, then pay with a valid test card.
    await fillBillingForm(page, { ...VALID_FR_ADDRESS, vatNumber: 'FR12345678901' });
    await fillStripeCard(page, TEST_CARDS.ok);

    await submitCheckout(page);
    await page.waitForURL(/.*\/checkout\/success\//, { timeout: 60_000 });

    // The finalize request body must carry the VAT number verbatim. (Whether a
    // reverse charge applies is Stripe's decision; here we only prove transmission.)
    const finalize = requests.find(
      r => r.url.endsWith('/api/v1/payments/finalize') && r.method === 'POST',
    );
    expect(finalize, 'a POST /payments/finalize should have been sent').toBeTruthy();
    expect(finalize!.body).toContain('"vatNumber":"FR12345678901"');
  });
});
