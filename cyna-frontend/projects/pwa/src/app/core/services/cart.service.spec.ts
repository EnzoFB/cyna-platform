import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';
import { ProductDetail } from '../../features/catalog/models/product.model';

describe('CartService', () => {
  let service: CartService;

  const product = (id: string): ProductDetail => ({
    id,
    translations: {
      fr: {
        name: `Product ${id}`,
        serviceDescription: '',
        technicalDescription: '',
        highlightPoints: [],
      }
    },
    categoryId: '00000000-0000-0000-0000-000000000001',
    categoryName: 'SOC',
    priorityLevel: 1,
    monthlyPrice: 100,
    annualPrice: 1000,
    currency: 'EUR',
    primaryImageBase64: null,
    isPublished: true,
    isAvailable: true,
    freeTrialDays: 0,
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

  describe('HT subtotal (cart never computes VAT — Stripe Tax does at checkout)', () => {
    it('is zero on an empty cart', () => {
      expect(service.subtotalHt()).toBe(0);
    });

    it('sums each line at its unit price × quantity, rounded to 2 decimals', () => {
      // 2× MONTHLY @100 = 200 HT
      service.addProduct(product('a'), 'MONTHLY', 2);

      expect(service.subtotalHt()).toBeCloseTo(200, 2);
      const subtotal = service.subtotalHt();
      expect(Math.round(subtotal * 100) / 100).toBe(subtotal);
    });
  });

  describe('per-cycle HT totals', () => {
    it('reports MIXED mode and per-cycle HT subtotals when cart has both cycles', () => {
      // 2× MONTHLY @100 = 200 HT ; 1× ANNUAL @1000 = 1000 HT
      service.addProduct(product('a'), 'MONTHLY', 2);
      service.addProduct(product('b'), 'ANNUAL', 1);

      expect(service.cartCycleMode()).toBe('MIXED');
      expect(service.monthlyTotalHt()).toBeCloseTo(200, 2);
      expect(service.annualTotalHt()).toBeCloseTo(1000, 2);
      // The two add up to the global HT subtotal.
      expect(service.subtotalHt()).toBeCloseTo(
        service.monthlyTotalHt() + service.annualTotalHt(), 2
      );
    });

    it('reports MONTHLY mode and zero annual HT when cart is monthly only', () => {
      service.addProduct(product('a'), 'MONTHLY', 1);

      expect(service.cartCycleMode()).toBe('MONTHLY');
      expect(service.monthlyTotalHt()).toBeGreaterThan(0);
      expect(service.annualTotalHt()).toBe(0);
    });

    it('reports ANNUAL mode and zero monthly HT when cart is annual only', () => {
      service.addProduct(product('a'), 'ANNUAL', 1);

      expect(service.cartCycleMode()).toBe('ANNUAL');
      expect(service.monthlyTotalHt()).toBe(0);
      expect(service.annualTotalHt()).toBeGreaterThan(0);
    });
  });
});
