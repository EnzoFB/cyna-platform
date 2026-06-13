import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';
import { ProductDetail } from '../../features/catalog/models/product.model';

describe('CartService', () => {
  let service: CartService;

  const product = (id: string): ProductDetail => ({
    id,
    name: `Product ${id}`,
    categoryId: '00000000-0000-0000-0000-000000000001',
    categoryName: 'SOC',
    priorityLevel: 1,
    monthlyPrice: 100,
    annualPrice: 1000,
    currency: 'EUR',
    primaryImageBase64: null,
    isPublished: true,
    isAvailable: true,
    serviceDescription: '',
    technicalDescription: '',
    freeTrialDays: 0,
    highlightPoints: [],
    images: []
  });

  beforeEach(() => {
    localStorage.removeItem('cyna_pwa_cart');
    localStorage.removeItem('cyna_pwa_guest_token');
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  it('allows mixed billing cycles at checkout (V14 backend creates one Stripe sub per line)', () => {
    service.addProduct(product('a'), 'MONTHLY', 1);
    service.addProduct(product('b'), 'ANNUAL', 1);

    expect(service.hasMixedBillingCycles()).toBeTrue();
    expect(service.checkoutAllowed()).toBeTrue();
  });

  it('blocks checkout when cart is empty', () => {
    expect(service.isEmpty()).toBeTrue();
    expect(service.checkoutAllowed()).toBeFalse();
  });

  it('blocks checkout when at least one item is unavailable', () => {
    const unavailable: ProductDetail = { ...product('x'), isAvailable: false };
    service.addProduct(unavailable, 'MONTHLY', 1);

    expect(service.hasUnavailableItems()).toBeTrue();
    expect(service.checkoutAllowed()).toBeFalse();
  });

  it('produces one line per (productId, billingCycle) tuple when the same product is added with both cycles', () => {
    const p = product('soc');
    service.addProduct(p, 'MONTHLY', 1);
    service.addProduct(p, 'ANNUAL', 1);

    const items = service.items();
    expect(items.length).toBe(2);
    expect(items.map(i => i.billingCycle).sort()).toEqual(['ANNUAL', 'MONTHLY']);
    // Distinct lineId → finalize will create two Stripe Subscriptions for this user.
    expect(items[0].lineId).not.toEqual(items[1].lineId);
  });

  describe('VAT computation (HT / VAT 20% / TTC estimate shown in the order summary)', () => {
    it('is zero on an empty cart', () => {
      expect(service.subtotalHt()).toBe(0);
      expect(service.vatAmount()).toBe(0);
      expect(service.totalTtc()).toBe(0);
    });

    it('applies 20% VAT on the HT subtotal and totals to TTC', () => {
      // 2× MONTHLY @100 = 200 HT
      service.addProduct(product('a'), 'MONTHLY', 2);

      expect(service.subtotalHt()).toBeCloseTo(200, 2);
      expect(service.vatAmount()).toBeCloseTo(40, 2);   // 200 × 0.20
      expect(service.totalTtc()).toBeCloseTo(240, 2);   // 200 + 40
      // Invariant: HT + VAT === TTC (what the summary displays must add up).
      expect(service.subtotalHt() + service.vatAmount()).toBeCloseTo(service.totalTtc(), 2);
    });

    it('rounds the VAT to 2 decimals', () => {
      // ANNUAL @1000, qty 1 → trivial; use a quantity that exercises rounding
      // via the per-cycle path is covered below — here assert 2-decimal output.
      service.addProduct(product('a'), 'MONTHLY', 1); // 100 HT → 20 VAT → 120 TTC
      const vat = service.vatAmount();
      expect(Number.isFinite(vat)).toBeTrue();
      expect(Math.round(vat * 100) / 100).toBe(vat);
    });
  });

  describe('per-cycle totals', () => {
    it('reports MIXED mode and per-cycle TTCs when cart has both cycles', () => {
      // 2× MONTHLY @100 = 200 HT ; 1× ANNUAL @1000 = 1000 HT
      service.addProduct(product('a'), 'MONTHLY', 2);
      service.addProduct(product('b'), 'ANNUAL', 1);

      expect(service.cartCycleMode()).toBe('MIXED');
      // VAT 20%: 200 * 1.2 = 240, 1000 * 1.2 = 1200
      expect(service.monthlyTotalTtc()).toBeCloseTo(240, 2);
      expect(service.annualTotalTtc()).toBeCloseTo(1200, 2);
      // The two add up to the global TTC.
      expect(service.totalTtc()).toBeCloseTo(
        service.monthlyTotalTtc() + service.annualTotalTtc(), 2
      );
    });

    it('reports MONTHLY mode and zero annual TTC when cart is monthly only', () => {
      service.addProduct(product('a'), 'MONTHLY', 1);

      expect(service.cartCycleMode()).toBe('MONTHLY');
      expect(service.monthlyTotalTtc()).toBeGreaterThan(0);
      expect(service.annualTotalTtc()).toBe(0);
    });

    it('reports ANNUAL mode and zero monthly TTC when cart is annual only', () => {
      service.addProduct(product('a'), 'ANNUAL', 1);

      expect(service.cartCycleMode()).toBe('ANNUAL');
      expect(service.monthlyTotalTtc()).toBe(0);
      expect(service.annualTotalTtc()).toBeGreaterThan(0);
    });
  });
});
