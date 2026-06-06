import { expect, test } from '@playwright/test';
import { loginViaToken } from '../helpers/auth';
import { registerUser } from '../helpers/api';

test.describe('PWA authenticated account journeys', () => {
  test('covers account tabs, profile update, address CRUD and logout', async ({ page }) => {
    const user = await registerUser('-account');
    await loginViaToken(page, user);

    await page.goto('/account');
    await expect(page.locator('.account-page')).toBeVisible();

    const tabs = page.locator('.account-tabs button');
    await expect(tabs).toHaveCount(5);

    await tabs.nth(1).click();
    await expect(page.locator('.history-item')).toHaveCount(0);
    await expect(page.locator('.tab-empty').first()).toBeVisible();

    await tabs.nth(2).click();
    const profileSection = page.locator('.profile-section').first();
    const profileForm = profileSection.locator('form.profile-form').first();
    await expect(profileForm.locator('#p-firstName')).toHaveValue('PW');
    await expect(profileForm.locator('#p-lastName')).toHaveValue('Test');
    await expect(profileForm.locator('#p-email')).toHaveValue(user.email);

    await profileForm.locator('#p-email').fill('bad-email');
    await profileForm.locator('#p-email').blur();
    await expect(profileSection.locator('.field-error')).toBeVisible();

    await profileForm.locator('#p-email').fill(user.email);
    await profileForm.locator('#p-company').fill('E2E Company');
    await profileForm.locator('#p-company').blur();
    await expect(profileForm.locator('button[type="submit"]')).toBeEnabled();
    const profileSave = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/account/profile')
        && response.request().method() === 'PATCH',
    );
    await profileForm.locator('button[type="submit"]').click();
    expect((await profileSave).status()).toBe(200);

    await page.reload();
    await tabs.nth(2).click();
    await expect(page.locator('#p-company')).toHaveValue('E2E Company');

    const passwordSection = page.locator('.profile-section--password');
    await passwordSection.locator('#p-currentPw').fill(user.password);
    await passwordSection.locator('#p-newPw').fill('Different!2026');
    await passwordSection.locator('#p-confirmPw').fill('Mismatch!2026');
    await passwordSection.locator('#p-confirmPw').blur();
    await expect(passwordSection.locator('.field-error')).toBeVisible();

    await tabs.nth(3).click();
    await expect(page.locator('.tab-empty')).toBeVisible();
    await page.locator('.btn-add').click();

    const addressModal = page.locator('.modal-backdrop');
    await expect(addressModal).toBeVisible();
    await addressModal.locator('#addr-firstName').fill('Jean');
    await addressModal.locator('#addr-lastName').fill('Dupont');
    await addressModal.locator('#addr-label').fill('Bureau');
    await addressModal.locator('#addr-address').fill('12 rue de la Paix');
    await addressModal.locator('#addr-city').fill('Paris');
    await addressModal.locator('#addr-zip').fill('75001');
    await addressModal.locator('#addr-region').fill('Ile-de-France');
    await addressModal.locator('.input-group-wrapper:has(.arrow) input').fill('France');
    await addressModal.locator('.country-autocomplete-option').first().click();
    await addressModal.locator('#addr-phone').fill('+33612345678');

    const createAddress = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/account/addresses')
        && response.request().method() === 'POST',
    );
    await addressModal.locator('button[type="submit"]').click();
    expect((await createAddress).status()).toBe(200);

    const firstCard = page.locator('.address-card').first();
    await expect(firstCard).toContainText('Bureau');

    await firstCard.locator('.btn-action').first().click();
    await expect(addressModal).toBeVisible();
    await addressModal.locator('#addr-label').fill('Siege');
    const updateAddress = page.waitForResponse(
      (response) =>
        /\/api\/v1\/account\/addresses\/.+/.test(response.url())
        && response.request().method() === 'PUT',
    );
    await addressModal.locator('button[type="submit"]').click();
    expect((await updateAddress).status()).toBe(200);
    await expect(firstCard).toContainText('Siege');

    const deleteAddress = page.waitForResponse(
      (response) =>
        /\/api\/v1\/account\/addresses\/.+/.test(response.url())
        && response.request().method() === 'DELETE',
    );
    await firstCard.locator('.btn-action--danger').click();
    expect((await deleteAddress).status()).toBe(200);
    await expect(page.locator('.tab-empty')).toBeVisible();

    await tabs.nth(4).click();
    await expect(page.locator('.portal-banner__cta')).toBeVisible();
    const billingPortal = page.waitForResponse(
      (response) =>
        response.url().includes('/api/v1/payments/billing-portal')
        && response.request().method() === 'POST',
    );
    await page.locator('.portal-banner__cta').click();
    expect((await billingPortal).status()).toBe(404);

    await page.locator('button.burger').click();
    await page.locator('.user-menu button.logout').click();
    await page.waitForURL(/\/auth\/login(?:\?.*)?$/, { timeout: 10_000 });
  });
});
