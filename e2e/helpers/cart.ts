import { Page, expect } from '@playwright/test';

export type BillingCycle = 'MONTHLY' | 'ANNUAL';

/**
 * Known-available product IDs from the seed data (V1__initial_complete_schema.sql).
 * These are products with is_available = true and is_published = true.
 * Using direct IDs makes tests deterministic — the catalog grid order can vary
 * for products that share the same priority_level.
 */
export const AVAILABLE_PRODUCT_IDS = [
  '10000000-0000-0000-0000-000000000001', // SOC Starter
  '20000000-0000-0000-0000-000000000001', // EDR Essential
  '30000000-0000-0000-0000-000000000001', // XDR Core
  '10000000-0000-0000-0000-000000000002', // SOC Advanced
  '20000000-0000-0000-0000-000000000002', // EDR Professional
  '30000000-0000-0000-0000-000000000002', // XDR Advanced
] as const;

/**
 * Adds an available product to the cart with the requested billing cycle.
 * `index` selects which product from AVAILABLE_PRODUCT_IDS (default: first).
 */
export async function addProductToCart(
  page: Page,
  options: { index?: number; billingCycle?: BillingCycle } = {}
): Promise<void> {
  const productId = AVAILABLE_PRODUCT_IDS[options.index ?? 0];
  const billingCycle = options.billingCycle ?? 'MONTHLY';

  await page.goto(`/catalog/${productId}`);
  await page.waitForLoadState('networkidle');
  await expect(page.locator('.pricing-card__cta')).toBeVisible({ timeout: 10_000 });

  // Toggle annual billing if requested.
  if (billingCycle === 'ANNUAL') {
    const toggle = page.locator('.pricing-card__billing-toggle');
    if (await toggle.count() > 0) {
      const pressed = await toggle.getAttribute('aria-pressed');
      if (pressed !== 'true') {
        await toggle.click();
      }
    }
  }

  // Click "S'ABONNER MAINTENANT".
  await page.locator('.pricing-card__cta').click();
  // Brief settle for the cart signal to update + toast to appear.
  await page.waitForTimeout(400);
}

export async function goToCart(page: Page): Promise<void> {
  await page.goto('/cart');
  await page.waitForLoadState('networkidle');
}

export async function clickCheckoutFromCart(page: Page): Promise<void> {
  const btn = page.locator('.cart-summary__checkout');
  await expect(btn).toBeEnabled({ timeout: 10_000 });
  await btn.click();
  await page.waitForLoadState('networkidle');
}
