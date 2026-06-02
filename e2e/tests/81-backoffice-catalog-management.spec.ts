import { expect, Page, test } from '@playwright/test';
import { createAdminUser } from '../helpers/admin';
import { loginViaToken } from '../helpers/auth';

function uniqueSuffix(): string {
  return `${Date.now()}-${Math.floor(Math.random() * 1000)}`;
}

async function fillCategoryModal(page: Page, technicalName: string, fullNameFr: string, fullNameEn: string) {
  const modal = page.locator('.modal-overlay').last();
  await expect(modal).toBeVisible();
  await modal.locator('#name').fill(technicalName);
  await modal.locator('div[formgroupname="fr"] input').fill(fullNameFr);
  await modal.locator('button.locale-tab').nth(1).click();
  await modal.locator('div[formgroupname="en"] input').fill(fullNameEn);
  return modal;
}

async function selectCategoryInProductModal(page: Page, categoryName: string) {
  const modal = page.locator('.modal-overlay').last();
  await modal.locator('input.custom-select__input').click();
  await modal.locator('.custom-dropdown__option', { hasText: categoryName }).click();
}

test.describe('Backoffice catalog management', () => {
  test('covers category, product and promotion CRUD flows', async ({ page }) => {
    test.setTimeout(180_000);

    const suffix = uniqueSuffix();
    const categoryTechnicalName = `e2e-cat-${suffix}`;
    const categoryNameFr = `E2E Cat ${suffix}`;
    const categoryNameFrUpdated = `${categoryNameFr} Update`;
    const categoryNameEn = `E2E Category ${suffix}`;
    const productNameFr = `E2E Product ${suffix}`;
    const productNameEn = `E2E Product EN ${suffix}`;

    const admin = await createAdminUser('-bo-catalog');
    await loginViaToken(page, admin);

    await page.goto('/categories');
    await expect(page.locator('.category-list')).toBeVisible();

    await page.locator('.btn-primary').click();
    let modal = await fillCategoryModal(page, categoryTechnicalName, categoryNameFr, categoryNameEn);
    const createCategory = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/categories')
        && response.request().method() === 'POST',
    );
    await modal.locator('button[type="submit"]').click();
    expect([200, 201]).toContain((await createCategory).status());

    await page.locator('.toolbar__search input').fill(categoryNameFr);
    const categoryRow = page.locator('.category-row', { hasText: categoryNameFr }).first();
    await expect(categoryRow).toBeVisible();

    await categoryRow.locator('.action-btn--edit').click();
    modal = page.locator('.modal-overlay').last();
    await modal.locator('div[formgroupname="fr"] input').fill(categoryNameFrUpdated);
    await modal.locator('button.locale-tab').nth(1).click();
    await modal.locator('div[formgroupname="en"] input').fill(`${categoryNameEn} Updated`);
    const updateCategory = page.waitForResponse(
      (response) =>
        /\/api\/v1\/categories\/.+/.test(response.url())
        && response.request().method() === 'PUT',
    );
    await modal.locator('button[type="submit"]').click();
    expect((await updateCategory).status()).toBe(200);

    await expect(page.locator('.category-row', { hasText: categoryNameFrUpdated }).first()).toBeVisible();

    await page.goto('/products');
    await expect(page.locator('.product-list')).toBeVisible();
    await page.locator('.btn-primary').click();

    modal = page.locator('.modal-overlay').last();
    await selectCategoryInProductModal(page, categoryNameFrUpdated);
    await modal.locator('#monthlyPrice').fill('49.99');
    await modal.locator('#annualPrice').fill('499.99');
    await modal.locator('#priorityLevel').fill('7');
    await modal.locator('#freeTrialDays').fill('14');
    await modal.locator('div[formgroupname="fr"] input').fill(productNameFr);
    await modal.locator('div[formgroupname="fr"] textarea').nth(0).fill('Description commerciale FR');
    await modal.locator('div[formgroupname="fr"] textarea').nth(1).fill('Description technique FR');
    await modal.locator('button.locale-tab').nth(1).click();
    await modal.locator('div[formgroupname="en"] input').fill(productNameEn);
    await modal.locator('div[formgroupname="en"] textarea').nth(0).fill('Commercial description EN');
    await modal.locator('div[formgroupname="en"] textarea').nth(1).fill('Technical description EN');

    const createProduct = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/products')
        && response.request().method() === 'POST',
    );
    await modal.locator('button[type="submit"]').click();
    expect([200, 201]).toContain((await createProduct).status());

    await page.locator('.toolbar__search input').fill(productNameFr);
    const productRow = page.locator('.product-row', { hasText: productNameFr }).first();
    await expect(productRow).toBeVisible();

    await productRow.locator('.action-btn--edit').click();
    modal = page.locator('.modal-overlay').last();
    await modal.locator('.custom-select__trigger').nth(0).click();
    await modal.locator('.custom-dropdown__option', { hasText: 'Publié' }).click();
    const updateProduct = page.waitForResponse(
      (response) =>
        /\/api\/v1\/products\/.+/.test(response.url())
        && response.request().method() === 'PUT',
    );
    await modal.locator('button[type="submit"]').click();
    expect((await updateProduct).status()).toBe(200);

    await expect(page.locator('.product-row', { hasText: productNameFr }).locator('.status-badge', { hasText: 'Publié' })).toBeVisible();

    await page.goto('/promotions');
    await expect(page.locator('.promotion-page__header')).toBeVisible();
    await page.locator('button.btn-primary', { hasText: 'Nouvelle promotion' }).click();

    modal = page.locator('.modal-overlay').last();
    await modal.locator('.product-picker__input').fill(productNameFr);
    await modal.locator('.product-picker__result-item', { hasText: productNameFr }).click();
    await modal.locator('input[type="number"]').first().fill('15');
    await modal.locator('.locale-content:not(.locale-content--hidden) textarea').fill('Promo FR e2e');
    await modal.locator('button.locale-tab').nth(1).click();
    await modal.locator('.locale-content:not(.locale-content--hidden) textarea').fill('Promo EN e2e');

    const createPromotion = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/admin/promotions')
        && response.request().method() === 'POST',
    );
    await modal.locator('button.btn-primary', { hasText: 'Enregistrer' }).click();
    expect([200, 201]).toContain((await createPromotion).status());

    const promotionRow = page.locator('tr', { hasText: productNameFr }).first();
    await expect(promotionRow).toContainText('-15%');
    await promotionRow.locator('button.btn-ghost').click();

    modal = page.locator('.modal-overlay').last();
    await modal.locator('input[type="number"]').first().fill('25');
    const updatePromotion = page.waitForResponse(
      (response) =>
        /\/api\/v1\/admin\/promotions\/.+/.test(response.url())
        && response.request().method() === 'PUT',
    );
    await modal.locator('button.btn-primary', { hasText: 'Enregistrer' }).click();
    expect((await updatePromotion).status()).toBe(200);
    await expect(promotionRow).toContainText('-25%');

    page.once('dialog', (dialog) => dialog.accept());
    const deletePromotion = page.waitForResponse(
      (response) =>
        /\/api\/v1\/admin\/promotions\/.+/.test(response.url())
        && response.request().method() === 'DELETE',
    );
    await promotionRow.locator('button.btn-danger').click();
    expect([200, 204]).toContain((await deletePromotion).status());
    await expect(page.locator('tr', { hasText: productNameFr })).toHaveCount(0);

    await page.goto('/products');
    await page.locator('.toolbar__search input').fill(productNameFr);
    const productRowAfter = page.locator('.product-row', { hasText: productNameFr }).first();
    await productRowAfter.locator('.action-btn--delete').click();
    modal = page.locator('.confirm-overlay').last();
    const deleteProduct = page.waitForResponse(
      (response) =>
        /\/api\/v1\/products\/.+/.test(response.url())
        && response.request().method() === 'DELETE',
    );
    await modal.locator('.btn-delete-confirm').click();
    expect([200, 204]).toContain((await deleteProduct).status());
    await expect(page.locator('.product-row', { hasText: productNameFr })).toHaveCount(0);

    await page.goto('/categories');
    await page.locator('.toolbar__search input').fill(categoryNameFrUpdated);
    const categoryRowAfter = page.locator('.category-row', { hasText: categoryNameFrUpdated }).first();
    await categoryRowAfter.locator('.action-btn--delete').click();
    modal = page.locator('.confirm-overlay').last();
    const deleteCategory = page.waitForResponse(
      (response) =>
        /\/api\/v1\/categories\/.+/.test(response.url())
        && response.request().method() === 'DELETE',
    );
    await modal.locator('.confirm-btn--danger').click();
    expect([200, 204]).toContain((await deleteCategory).status());
    await expect(page.locator('.category-row', { hasText: categoryNameFrUpdated })).toHaveCount(0);
  });
});
