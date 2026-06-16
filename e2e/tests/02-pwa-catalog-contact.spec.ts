import { expect, test } from '@playwright/test';

test.describe('PWA catalog and public forms', () => {
  test('supports catalog search, filters and empty states', async ({ page }) => {
    await page.goto('/catalog');

    await expect(page.locator('.catalog-page')).toBeVisible();
    await expect(page.locator('.catalog-grid__item').first()).toBeVisible();
    const initialCount = await page.locator('.catalog-grid__item').count();

    const searchInput = page.locator('.catalog-search input[type="search"]');
    const searchResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/products')
        && response.url().includes('search=SOC')
        && response.request().method() === 'GET',
    );
    await searchInput.fill('SOC');
    await searchResponse;
    const filteredCount = await page.locator('.catalog-grid__item').count();
    expect(filteredCount).toBeGreaterThan(0);
    expect(filteredCount).toBeLessThan(initialCount);
    await expect(page.locator('.product-card__name').first()).toContainText(/SOC/i);

    const clearSearchResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/products')
        && !response.url().includes('search=')
        && response.request().method() === 'GET',
    );
    await page.locator('.catalog-search__clear').click();
    await clearSearchResponse;

    await page.locator('input[name="category"]').nth(1).check();
    await expect(page.locator('.category-hero')).toBeVisible();
    expect(await page.locator('.catalog-grid__item').count()).toBeGreaterThan(0);

    const sortResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/products')
        && response.request().method() === 'GET',
    );
    await page.locator('#sort-price-desc').check();
    await sortResponse;
    const prices = await page.locator('.product-card__price').allTextContents();
    expect(prices.length).toBeGreaterThan(0);

    const emptySearchResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/products')
        && response.url().includes('unlikely-search-term-e2e')
        && response.request().method() === 'GET',
    );
    await searchInput.fill('unlikely-search-term-e2e');
    await emptySearchResponse;
    await expect(page.locator('.catalog-results__empty')).toBeVisible();
  });

  test('validates and submits the contact form', async ({ page }) => {
    await page.goto('/contact');

    await page.locator('button[type="submit"]').click();
    await expect(page.locator('.field-error')).toHaveCount(4);

    await page.fill('#contact-name', 'E2E Contact');
    await page.fill('#contact-email', 'invalid-email');
    await page.fill('#contact-subject', 'Demande E2E');
    await page.fill('#contact-message', 'Message de test');
    await page.locator('button[type="submit"]').click();
    await expect(page.locator('.field-error')).toHaveCount(1);

    await page.fill('#contact-email', 'e2e-contact@cyna.test');
    const submitResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/contact')
        && response.request().method() === 'POST',
    );

    await page.locator('button[type="submit"]').click();
    const response = await submitResponse;

    expect(response.status()).toBe(200);
    await expect(page.locator('.contact-success')).toBeVisible();
  });
});
