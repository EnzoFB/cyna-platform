import { expect, test } from '@playwright/test';
import { createAdminUser } from '../helpers/admin';
import { loginViaToken } from '../helpers/auth';
import { ensureDashboardSeedData } from '../helpers/db';

function createUniqueEmail(): string {
  return `bo-user-${Date.now()}-${Math.floor(Math.random() * 1000)}@cyna.test`;
}

test.describe('Backoffice user and order operations', () => {
  test('covers dashboard navigation, users CRUD, orders search, filter, detail and pagination', async ({ page }) => {
    test.setTimeout(120_000);

    await ensureDashboardSeedData();

    const admin = await createAdminUser('-bo-ops');
    await loginViaToken(page, admin);

    await page.goto('/');
    await expect(page.locator('.shell')).toBeVisible();
    await expect(page.locator('.sidebar')).toContainText('Dashboard');

    const sidebarLinks = page.locator('.sidebar__link');
    await sidebarLinks.nth(4).click();
    await expect(page).toHaveURL(/\/users$/);
    await expect(page.locator('.user-list')).toBeVisible();

    await page.locator('.pagination__size select').selectOption('100');
    await expect(page.locator('.pagination__pages')).toContainText('1 /');

    const createdEmail = createUniqueEmail();
    await page.locator('.btn-primary', { hasText: 'Nouveau' }).click();
    let modal = page.locator('.modal-overlay').last();
    await modal.locator('#email').fill(createdEmail);
    await modal.locator('#password').fill('TestPwd!2026');
    await modal.locator('#firstName').fill('Back');
    await modal.locator('#lastName').fill('Office');
    await modal.locator('#role').click();
    await modal.locator('.custom-dropdown__option', { hasText: 'Support' }).click();
    const createUser = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/admin/users')
        && response.request().method() === 'POST',
    );
    await modal.locator('button[type="submit"]').click();
    expect((await createUser).status()).toBe(201);

    const createdRow = page.locator('.user-row', { hasText: createdEmail }).first();
    await expect(createdRow).toBeVisible();

    await page.locator('.toolbar__search input').fill(createdEmail);
    await expect(page.locator('.user-row')).toHaveCount(1);

    await createdRow.locator('.action-btn--edit').click();
    modal = page.locator('.modal-overlay').last();
    await modal.locator('#status').click();
    await modal.locator('.custom-dropdown__option', { hasText: 'Inactif' }).click();
    const updateUser = page.waitForResponse(
      (response) =>
        /\/api\/v1\/admin\/users\/.+/.test(response.url())
        && response.request().method() === 'PUT',
    );
    await modal.locator('button[type="submit"]').click();
    expect((await updateUser).status()).toBe(200);
    await expect(createdRow.locator('.status-badge')).toContainText('INACTIVE');

    page.once('dialog', (dialog) => dialog.accept());
    const deleteUser = page.waitForResponse(
      (response) =>
        /\/api\/v1\/admin\/users\/.+/.test(response.url())
        && response.request().method() === 'DELETE',
    );
    await createdRow.locator('.action-btn--delete').click();
    expect((await deleteUser).status()).toBe(200);
    await expect(page.locator('.user-row', { hasText: createdEmail })).toHaveCount(0);

    await sidebarLinks.nth(5).click();
    await expect(page).toHaveURL(/\/orders$/);
    await expect(page.locator('.order-list')).toBeVisible();
    await expect(page.locator('.pagination__pages')).toContainText('1 /');

    const firstOrderEmail = (await page.locator('.order-row').first().locator('.cell-customer__email').textContent())?.trim();
    expect(firstOrderEmail).toBeTruthy();

    await page.locator('.toolbar__search input').fill(firstOrderEmail!);
    await expect(page.locator('.order-row')).toHaveCount(1);

    await page.locator('.toolbar__search input').fill('');
    const filterResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/admin/orders')
        && response.url().includes('status=PAID')
        && response.request().method() === 'GET',
    );
    await page.locator('.custom-select__trigger').click();
    await page.locator('.custom-dropdown__option', { hasText: 'Payée' }).click();
    await filterResponse;
    expect(await page.locator('.order-row').count()).toBeGreaterThan(0);
    await expect(page.locator('.order-row').first()).toBeVisible();
    await expect(page.locator('.order-row').first().locator('.status-badge')).toContainText('Payée');

    const detailRequest = page.waitForResponse(
      (response) =>
        /\/api\/v1\/admin\/orders\/[0-9a-f-]+$/.test(response.url())
        && response.request().method() === 'GET',
    );
    await page.locator('.order-row').first().locator('.action-btn--view').click();
    expect((await detailRequest).status()).toBe(200);
    await expect(page.locator('.modal-card')).toBeVisible();
    await page.locator('.modal-card button', { hasText: 'Fermer' }).click();

    const resetFilterResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/admin/orders')
        && !response.url().includes('status=')
        && response.request().method() === 'GET',
    );
    await page.locator('.custom-select__trigger').click();
    await page.locator('.custom-dropdown__option', { hasText: 'Tous les statuts' }).click();
    await resetFilterResponse;

    const nextPageResponse = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/admin/orders')
        && response.url().includes('page=1')
        && response.request().method() === 'GET',
    );
    await page.locator('.pagination__nav .pagination__btn').nth(2).click();
    await nextPageResponse;
    await expect(page.locator('.pagination__pages')).toContainText('2 /');
  });
});
