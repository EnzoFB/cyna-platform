import { Page, expect } from '@playwright/test';

export interface BillingAddress {
  firstName: string;
  lastName: string;
  address: string;
  zipCode: string;
  city: string;
  region: string;
  countryName: string;
  phone: string;
  cardHolder: string;
}

export const VALID_FR_ADDRESS: BillingAddress = {
  firstName: 'Jean',
  lastName: 'Dupont',
  address: '12 rue de la Paix',
  zipCode: '75001',
  city: 'Paris',
  region: 'Île-de-France',
  countryName: 'France',
  phone: '+33612345678',
  cardHolder: 'JEAN DUPONT',
};

export interface StripeCard {
  number: string;     // formatted with or without spaces
  expiry: string;     // "MM / YY"
  cvc: string;        // 3 digits
}

export const TEST_CARDS = {
  ok: { number: '4242 4242 4242 4242', expiry: '12 / 30', cvc: '123' },
  declined: { number: '4000 0000 0000 0002', expiry: '12 / 30', cvc: '123' },
  insufficientFunds: { number: '4000 0000 0000 9995', expiry: '12 / 30', cvc: '123' },
  incorrectCvc: { number: '4000 0000 0000 0127', expiry: '12 / 30', cvc: '123' },
} as const;

/**
 * Fills the new-billing-address form on the checkout page.
 */
export async function fillBillingForm(page: Page, addr: BillingAddress = VALID_FR_ADDRESS): Promise<void> {
  await page.locator('input[formControlName="firstName"]').fill(addr.firstName);
  await page.locator('input[formControlName="lastName"]').fill(addr.lastName);
  await page.locator('input[formControlName="address"]').fill(addr.address);
  await page.locator('input[formControlName="zipCode"]').fill(addr.zipCode);
  await page.locator('input[formControlName="city"]').fill(addr.city);
  await page.locator('input[formControlName="region"]').fill(addr.region);

  // Country autocomplete (input has no formControlName)
  const countryGroup = page.locator('div.input-group-wrapper:has(label:has-text("Pays"))');
  await countryGroup.locator('input[type="text"]').fill(addr.countryName);
  await page.waitForTimeout(300);
  const option = page.locator('.country-autocomplete-option').filter({ hasText: new RegExp(`^${addr.countryName}$`) }).first();
  if (await option.count() > 0) {
    await option.click();
  }

  await page.locator('input[formControlName="phone"]').fill(addr.phone);
  await page.locator('input[formControlName="holder"]').fill(addr.cardHolder);
}

/**
 * Fills the three Stripe Elements iframes (number, expiry, cvc).
 * Stripe Elements are rendered as iframes inside #card-number-element etc.
 */
export async function fillStripeCard(page: Page, card: StripeCard = TEST_CARDS.ok): Promise<void> {
  // Wait for at least one iframe to mount in each Stripe Elements container.
  await page.locator('#card-number-element iframe').first().waitFor({ state: 'attached', timeout: 20_000 });
  await page.locator('#card-expiry-element iframe').first().waitFor({ state: 'attached', timeout: 5_000 });
  await page.locator('#card-cvc-element iframe').first().waitFor({ state: 'attached', timeout: 5_000 });

  const numberFrame = page.frameLocator('#card-number-element iframe').first();
  const expiryFrame = page.frameLocator('#card-expiry-element iframe').first();
  const cvcFrame = page.frameLocator('#card-cvc-element iframe').first();

  // Stripe input names are stable across versions.
  await numberFrame.locator('input[name="cardnumber"]').fill(card.number);
  await expiryFrame.locator('input[name="exp-date"]').fill(card.expiry);
  await cvcFrame.locator('input[name="cvc"]').fill(card.cvc);
}

/**
 * Captures every /api/v1 response that arrives during the test for diagnostics.
 */
export interface CapturedCall { url: string; status: number; body: string }
export function captureApi(page: Page): CapturedCall[] {
  const calls: CapturedCall[] = [];
  page.on('response', async (response) => {
    const url = response.url();
    if (!url.includes('/api/v1/')) return;
    let body = '';
    try { body = await response.text(); } catch { body = '<unreadable>'; }
    calls.push({ url, status: response.status(), body: body.slice(0, 800) });
  });
  return calls;
}

/**
 * Clicks the checkout submit button (whose label depends on billing cycle).
 */
export async function submitCheckout(page: Page): Promise<void> {
  await page.locator('.checkout-submit').click();
}
