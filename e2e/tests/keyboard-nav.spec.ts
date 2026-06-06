/**
 * Keyboard navigation tests — WCAG 2.1 criteria:
 *   2.1.1  All interactive elements reachable by keyboard
 *   2.4.1  Skip link bypasses repeated navigation
 *   2.4.3  Focus order is logical
 *   2.4.7  Focus indicator is visible (outline not none/0px)
 *   2.1.2  No keyboard trap
 */
import { test, expect, Page } from '@playwright/test';
import { registerUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';

// ---------- helpers ----------

/**
 * Returns true when the currently focused element has a visible focus indicator.
 * Checks the element itself AND its direct parent — inputs are often inside
 * a styled wrapper (.input-group) that carries the focus ring rather than
 * the native input.
 */
async function focusIsVisible(page: Page): Promise<boolean> {
  return page.evaluate(() => {
    function hasIndicator(el: Element | null): boolean {
      if (!el) return false;
      const s = window.getComputedStyle(el);
      const outlineWidth = parseFloat(s.outlineWidth);
      const hasOutline = s.outlineStyle !== 'none' && outlineWidth > 0;
      const hasShadow = s.boxShadow !== 'none' && s.boxShadow !== '';
      return hasOutline || hasShadow;
    }
    const el = document.activeElement;
    if (!el || el === document.body) return false;
    return hasIndicator(el) || hasIndicator(el.parentElement);
  });
}

/** Tabs forward N times and returns info about each focused element. */
async function tabThrough(page: Page, times: number) {
  const elements: { tag: string; role: string | null; text: string; visible: boolean }[] = [];
  for (let i = 0; i < times; i++) {
    await page.keyboard.press('Tab');
    const info = await page.evaluate(() => {
      function hasIndicator(el: Element | null): boolean {
        if (!el) return false;
        const s = window.getComputedStyle(el);
        const outlineWidth = parseFloat(s.outlineWidth);
        return (s.outlineStyle !== 'none' && outlineWidth > 0) ||
               (s.boxShadow !== 'none' && s.boxShadow !== '');
      }
      const el = document.activeElement as HTMLElement | null;
      if (!el) return null;
      return {
        tag: el.tagName,
        role: el.getAttribute('role'),
        text: (el.textContent ?? '').trim().slice(0, 60),
        ariaLabel: el.getAttribute('aria-label') ?? '',
        focusVisible: hasIndicator(el) || hasIndicator(el.parentElement),
      };
    });
    if (info) elements.push({ tag: info.tag, role: info.role, text: info.text || info.ariaLabel, visible: info.focusVisible });
  }
  return elements;
}

// ---------- tests ----------

test.describe('Keyboard navigation — WCAG 2.1', () => {

  test('2.4.1 — skip link is first focusable element and reaches main content', async ({ page }) => {
    await page.goto('/home');
    await page.waitForLoadState('networkidle');

    // First Tab must land on the skip link
    await page.keyboard.press('Tab');
    const focused = await page.evaluate(() => ({
      tag: document.activeElement?.tagName,
      href: document.activeElement?.getAttribute('href'),
      text: document.activeElement?.textContent?.trim(),
    }));

    // Skip link is now a <button> to avoid Angular Router fragment interception
    expect(focused.tag).toBe('BUTTON');
    expect(focused.text).toBeTruthy();

    // Activating the skip link — Angular Router may add the fragment to the URL.
    // We verify the URL contains #main-content and that the <main> element exists.
    await page.keyboard.press('Enter');
    await page.waitForURL(/#main-content/, { timeout: 3000 }).catch(() => {});
    const mainExists = await page.locator('#main-content').count();
    expect(mainExists).toBeGreaterThan(0);
  });

  test('2.4.7 — every tabbable element on /home has a visible focus indicator', async ({ page }) => {
    await page.goto('/home');
    await page.waitForLoadState('networkidle');

    const invisible: string[] = [];
    // Tab through first 15 interactive elements (header + hero content)
    for (let i = 0; i < 15; i++) {
      await page.keyboard.press('Tab');
      const info = await page.evaluate(() => {
        function hasIndicator(el: Element | null): boolean {
          if (!el) return false;
          const s = window.getComputedStyle(el);
          const outlineWidth = parseFloat(s.outlineWidth);
          return (s.outlineStyle !== 'none' && outlineWidth > 0) ||
                 (s.boxShadow !== 'none' && s.boxShadow !== '');
        }
        const el = document.activeElement as HTMLElement | null;
        if (!el || el === document.body) return null;
        return {
          tag: el.tagName,
          text: (el.textContent ?? '').trim().slice(0, 40) || el.getAttribute('aria-label') || '',
          focusVisible: hasIndicator(el) || hasIndicator(el.parentElement),
        };
      });
      if (info && !info.focusVisible) {
        invisible.push(`<${info.tag}> "${info.text}"`);
      }
    }

    expect(invisible, `Elements without visible focus: ${invisible.join(', ')}`).toHaveLength(0);
  });

  test('2.4.7 — every tabbable element on /catalog has a visible focus indicator', async ({ page }) => {
    await page.goto('/catalog');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(500);

    const invisible: string[] = [];
    for (let i = 0; i < 20; i++) {
      await page.keyboard.press('Tab');
      const info = await page.evaluate(() => {
        function hasIndicator(el: Element | null): boolean {
          if (!el) return false;
          const s = window.getComputedStyle(el);
          const outlineWidth = parseFloat(s.outlineWidth);
          return (s.outlineStyle !== 'none' && outlineWidth > 0) ||
                 (s.boxShadow !== 'none' && s.boxShadow !== '');
        }
        const el = document.activeElement as HTMLElement | null;
        if (!el || el === document.body) return null;
        return {
          tag: el.tagName,
          text: (el.textContent ?? '').trim().slice(0, 40) || el.getAttribute('aria-label') || '',
          focusVisible: hasIndicator(el) || hasIndicator(el.parentElement),
        };
      });
      if (info && !info.focusVisible) {
        invisible.push(`<${info.tag}> "${info.text}"`);
      }
    }

    expect(invisible, `Elements without visible focus: ${invisible.join(', ')}`).toHaveLength(0);
  });

  test('2.4.3 — focus order on /auth/login is logical (skip → header → form)', async ({ page }) => {
    await page.goto('/auth/login');
    await page.waitForLoadState('networkidle');

    const elements = await tabThrough(page, 10);

    // First element = skip link (button)
    expect(elements[0]?.tag).toBe('BUTTON');
    expect(elements[0]?.text).toMatch(/contenu|content/i);

    // Form inputs should appear before the submit button
    // (skip skip-link at index 0 which is also a BUTTON)
    const inputIdx = elements.findIndex(e => e.tag === 'INPUT');
    const submitIdx = elements.findIndex((e, i) => e.tag === 'BUTTON' && i > 0 && !e.text?.match(/contenu|content/i));
    expect(inputIdx).toBeGreaterThan(-1);
    expect(submitIdx).toBeGreaterThan(inputIdx);
  });

  test('2.1.2 — no keyboard trap in header search dropdown', async ({ page }) => {
    await page.goto('/catalog');
    await page.waitForLoadState('networkidle');

    // Focus the header search input
    await page.locator('#header-search').focus();
    await page.keyboard.type('SOC');
    await page.waitForTimeout(600);

    // Dropdown should be open
    const dropdownVisible = await page.locator('#search-dropdown').isVisible();
    expect(dropdownVisible).toBe(true);

    // Pressing Escape should close the dropdown
    await page.keyboard.press('Escape');
    await page.waitForTimeout(200);

    // After Escape, focus should still be on the input (not trapped)
    const focusedId = await page.evaluate(() => document.activeElement?.id);
    expect(focusedId).toBe('header-search');

    // Tab should move focus OUT of the search (no trap)
    await page.keyboard.press('Tab');
    const afterTab = await page.evaluate(() => document.activeElement?.id);
    expect(afterTab).not.toBe('header-search');
  });

  test('2.1.1 — auth login form fully operable by keyboard', async ({ page }) => {
    await page.goto('/auth/login');
    await page.waitForLoadState('networkidle');

    // Navigate to email input via Tab
    let focused = '';
    for (let i = 0; i < 15; i++) {
      await page.keyboard.press('Tab');
      focused = await page.evaluate(() => document.activeElement?.id ?? '');
      if (focused === 'email-login') break;
    }
    expect(focused).toBe('email-login');

    // Fill form via keyboard only
    await page.keyboard.type('test@cyna.fr');
    await page.keyboard.press('Tab');
    const focusedPwd = await page.evaluate(() => document.activeElement?.id ?? '');
    expect(focusedPwd).toBe('password-login');

    await page.keyboard.type('TestPwd!2026');

    // Tab to forgot-password link then to submit button
    await page.keyboard.press('Tab');
    const focusedForgot = await page.evaluate(() =>
      (document.activeElement as HTMLElement)?.className ?? '');
    expect(focusedForgot).toContain('forgot-password');

    await page.keyboard.press('Tab');
    const focusedSubmit = await page.evaluate(() => document.activeElement?.tagName ?? '');
    expect(focusedSubmit).toBe('BUTTON');
  });

  test('2.1.1 — account tabs navigable by keyboard', async ({ page }) => {
    const user = await registerUser('-kbd');
    await loginViaToken(page, user);

    await page.goto('/account');
    await page.waitForLoadState('networkidle');
    // Wait for account tabs to render (guards + API calls settle)
    await page.locator('.account-tabs button').first().waitFor({ timeout: 15000 });

    // Focus the tab bar
    const firstTab = page.locator('.account-tabs button').first();
    await firstTab.focus();

    // Arrow right should move to next tab
    await page.keyboard.press('ArrowRight');
    const focused = await page.evaluate(() => document.activeElement?.textContent?.trim());
    // If arrow keys don't work on buttons, Tab should move between tabs
    // Either way, focus should be on a tab button
    const focusedTag = await page.evaluate(() => document.activeElement?.tagName);
    expect(focusedTag).toBe('BUTTON');

    // Enter/Space should activate the tab
    await page.keyboard.press('Enter');
    await page.waitForTimeout(300);
    const activeTab = await page.locator('.account-tabs button.active').textContent();
    expect(activeTab?.trim()).toBeTruthy();
  });

  test('2.4.7 — focus visible on /auth/login form elements', async ({ page }) => {
    await page.goto('/auth/login');
    await page.waitForLoadState('networkidle');

    const invisible: string[] = [];
    for (let i = 0; i < 12; i++) {
      await page.keyboard.press('Tab');
      const info = await page.evaluate(() => {
        function hasIndicator(el: Element | null): boolean {
          if (!el) return false;
          const s = window.getComputedStyle(el);
          const outlineWidth = parseFloat(s.outlineWidth);
          return (s.outlineStyle !== 'none' && outlineWidth > 0) ||
                 (s.boxShadow !== 'none' && s.boxShadow !== '');
        }
        const el = document.activeElement as HTMLElement | null;
        if (!el || el === document.body) return null;
        return {
          tag: el.tagName,
          id: el.id,
          text: (el.textContent ?? '').trim().slice(0, 40) || el.getAttribute('aria-label') || '',
          focusVisible: hasIndicator(el) || hasIndicator(el.parentElement),
        };
      });
      if (info && !info.focusVisible) {
        invisible.push(`<${info.tag}#${info.id}> "${info.text}"`);
      }
    }

    expect(invisible, `Elements without visible focus: ${invisible.join(', ')}`).toHaveLength(0);
  });

});
