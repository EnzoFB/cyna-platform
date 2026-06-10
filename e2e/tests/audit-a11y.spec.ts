/**
 * Accessibility audit script using axe-core + Playwright.
 * Audits key pages of both PWA (4200) and Backoffice (4201).
 *
 * Run: npx ts-node --esm audit-a11y.ts  OR  npx playwright test audit-a11y.ts
 * (wrapped as a Playwright test so we can reuse the browser binary)
 */
import { test, expect, Page, request } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { registerUser, API_URL, RegisteredUser } from '../helpers/api';
import { loginViaToken } from '../helpers/auth';

/** Adds a product to the cart via API using the user's access token. */
async function addProductToCart(user: RegisteredUser, productId: string): Promise<void> {
  const ctx = await request.newContext();
  const res = await ctx.post(`${API_URL}/cart/lines`, {
    data: { productId, billingCycle: 'MONTHLY', quantity: 1 },
    headers: { Authorization: `Bearer ${user.accessToken}` },
  });
  await ctx.dispose();
  if (!res.ok()) {
    throw new Error(`addProductToCart failed: ${res.status()} ${await res.text()}`);
  }
}

// ---------- helpers ----------

interface PageAudit {
  url: string;
  label: string;
  violations: Violation[];
}

interface Violation {
  id: string;
  impact: string | null;
  description: string;
  help: string;
  helpUrl: string;
  nodes: number;
  html: string[];
}

async function auditPage(page: Page, url: string, label: string): Promise<PageAudit> {
  await page.context().setExtraHTTPHeaders({ 'Cache-Control': 'no-cache' });
  await page.goto(url, { waitUntil: 'networkidle', timeout: 30000 });
  // Let Angular finish rendering (guards, translate pipe, signals)
  await page.waitForTimeout(800);

  const results = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'])
    // These two rules fire on Stripe's injected iframe/inputs which we cannot
    // modify. aria-hidden-focus: Stripe places aria-hidden on internal OTP inputs
    // but keeps them focusable. frame-focusable-content: Stripe's payment iframe
    // uses tabindex=-1 on its root frame. Neither is under our control.
    .disableRules(['aria-hidden-focus', 'frame-focusable-content'])
    .analyze();

  const violations: Violation[] = results.violations.map(v => ({
    id: v.id,
    impact: v.impact ?? null,
    description: v.description,
    help: v.help,
    helpUrl: v.helpUrl,
    nodes: v.nodes.length,
    html: v.nodes.slice(0, 2).map(n => n.html),
  }));

  return { url, label, violations };
}

function printReport(audits: PageAudit[]) {
  const impactOrder: Record<string, number> = { critical: 0, serious: 1, moderate: 2, minor: 3 };

  let totalViolations = 0;
  const byRule: Record<string, { impact: string | null; pages: string[]; count: number }> = {};

  for (const audit of audits) {
    totalViolations += audit.violations.length;
    for (const v of audit.violations) {
      if (!byRule[v.id]) byRule[v.id] = { impact: v.impact, pages: [], count: 0 };
      byRule[v.id].pages.push(audit.label);
      byRule[v.id].count += v.nodes;
    }
  }

  console.log('\n\n══════════════════════════════════════════');
  console.log('       RAPPORT ACCESSIBILITÉ CYNA');
  console.log('══════════════════════════════════════════\n');

  // Per-page summary
  for (const audit of audits) {
    const icon = audit.violations.length === 0 ? '✅' : '❌';
    console.log(`${icon} ${audit.label} (${audit.url})`);
    if (audit.violations.length > 0) {
      const sorted = [...audit.violations].sort(
        (a, b) => (impactOrder[a.impact ?? ''] ?? 9) - (impactOrder[b.impact ?? ''] ?? 9)
      );
      for (const v of sorted) {
        const badge = { critical: '🔴', serious: '🟠', moderate: '🟡', minor: '⚪' }[v.impact ?? ''] ?? '⚫';
        console.log(`   ${badge} [${v.impact?.toUpperCase() ?? '?'}] ${v.id} — ${v.help} (${v.nodes} nœud(s))`);
        console.log(`      ${v.helpUrl}`);
        for (const html of v.html) {
          console.log(`      HTML: ${html.slice(0, 120)}`);
        }
      }
    }
    console.log('');
  }

  // Cross-page summary
  console.log('──────────────────────────────────────────');
  console.log(`TOTAL : ${totalViolations} règles WCAG violées sur ${audits.length} pages\n`);

  const sorted = Object.entries(byRule).sort(
    ([, a], [, b]) => (impactOrder[a.impact ?? ''] ?? 9) - (impactOrder[b.impact ?? ''] ?? 9)
  );
  for (const [ruleId, info] of sorted) {
    const badge = { critical: '🔴', serious: '🟠', moderate: '🟡', minor: '⚪' }[info.impact ?? ''] ?? '⚫';
    console.log(`${badge} ${ruleId} (${info.impact}) — ${info.count} nœud(s) sur : ${info.pages.join(', ')}`);
  }
  console.log('══════════════════════════════════════════\n');
}

// ---------- test ----------

test('Accessibility audit — PWA + Backoffice', async ({ page }) => {
  test.setTimeout(180_000); // 15 pages × ~10s each
  const PWA = 'http://localhost:4200';
  const BO  = 'http://localhost:4201';
  // A seed product always present (XDR Advanced)
  const PRODUCT_ID = '30000000-0000-0000-0000-000000000002';

  // Register a user once; reuse for all authenticated pages
  const user = await registerUser('-a11y');
  // Add a product to the cart so /cart and /checkout have content to render
  await addProductToCart(user, PRODUCT_ID);
  await loginViaToken(page, user);

  // Helper: switch account tab by index then audit
  async function accountTabSetup(tabIndex: number) {
    await page.goto(`${PWA}/account`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(600);
    const tabs = page.locator('.account-tabs button');
    await tabs.nth(tabIndex).click();
    await page.waitForTimeout(400);
  }

  const pages: Array<{ url: string; label: string; setup?: () => Promise<void> }> = [
    // --- PWA authenticated (session active — do these first) ---
    // Tabs order: 0=Abonnements 1=Historique 2=Profil 3=Adresses 4=Paiement
    { url: `${PWA}/cart`,             label: 'PWA · Panier' },
    { url: `${PWA}/checkout`,         label: 'PWA · Checkout' },
    { url: `${PWA}/account`,          label: 'PWA · Account — Abonnements (défaut)' },
    { url: `${PWA}/account`,          label: 'PWA · Account — Historique',
      setup: () => accountTabSetup(1) },
    { url: `${PWA}/account`,          label: 'PWA · Account — Profil',
      setup: () => accountTabSetup(2) },
    { url: `${PWA}/account`,          label: 'PWA · Account — Adresses',
      setup: () => accountTabSetup(3) },
    { url: `${PWA}/account`,          label: 'PWA · Account — Paiement',
      setup: () => accountTabSetup(4) },
    // --- PWA public (session not required) ---
    { url: `${PWA}/home`,             label: 'PWA · Accueil' },
    { url: `${PWA}/offers`,           label: 'PWA · Offres / Promotions' },
    { url: `${PWA}/catalog`,          label: 'PWA · Catalogue' },
    { url: `${PWA}/catalog/${PRODUCT_ID}`, label: 'PWA · Fiche produit' },
    { url: `${PWA}/contact`,          label: 'PWA · Contact' },
    { url: `${PWA}/legal-notice`,     label: 'PWA · Mentions légales' },
    { url: `${PWA}/terms`,            label: 'PWA · CGU' },
    { url: `${PWA}/about`,            label: 'PWA · À propos' },
    { url: `${PWA}/auth/login`,       label: 'PWA · Login' },
    // --- Backoffice ---
    { url: `${BO}/`,                  label: 'BO · Login admin' },
  ];

  const audits: PageAudit[] = [];

  for (const p of pages) {
    // setup() handles navigation + tab switch for multi-state pages
    // auditPage() navigates normally for standard pages
    if (p.setup) {
      await p.setup();
      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'])
        .analyze();
      const violations: Violation[] = results.violations.map(v => ({
        id: v.id,
        impact: v.impact ?? null,
        description: v.description,
        help: v.help,
        helpUrl: v.helpUrl,
        nodes: v.nodes.length,
        html: v.nodes.slice(0, 2).map(n => n.html),
      }));
      const audit: PageAudit = { url: p.url, label: p.label, violations };
      audits.push(audit);
      console.log(`Audited: ${p.label} — ${violations.length} violation(s)`);
      continue;
    }
    const audit = await auditPage(page, p.url, p.label);
    audits.push(audit);
    console.log(`Audited: ${p.label} — ${audit.violations.length} violation(s)`);
  }

  printReport(audits);

  // Write JSON results for later processing
  const fs = await import('fs');
  fs.writeFileSync(
    'a11y-results.json',
    JSON.stringify(audits, null, 2),
    'utf-8'
  );

  // Don't fail the test — we're auditing, not asserting zero violations
  expect(true).toBe(true);
});
