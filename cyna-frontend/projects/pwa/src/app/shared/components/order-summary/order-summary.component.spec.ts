import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { OrderSummaryComponent } from './order-summary.component';

describe('OrderSummary', () => {
  let component: OrderSummaryComponent;
  let fixture: ComponentFixture<OrderSummaryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderSummaryComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OrderSummaryComponent);
    component = fixture.componentInstance;
    // `summary` is a required @Input — provide a minimal valid value so the
    // template can render in the smoke test.
    component.summary = {
      items: [],
      subtotalHt: 0,
      currency: 'EUR',
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows only the HT subtotal + "VAT at checkout" disclaimer when no VAT data is provided (cart page)', () => {
    component.summary = {
      items: [{ label: 'SOC', quantity: 1, billingCycle: 'MONTHLY', total: 100 }],
      subtotalHt: 100,
      currency: 'EUR',
    };
    fixture.detectChanges();

    const lines = fixture.nativeElement.querySelectorAll('.cart-summary__total-line');
    // Two rows: HT subtotal + grand total (= subtotal, no VAT row).
    expect(lines.length).toBe(2);
    expect(lines[0].textContent).toContain('100');
    expect(lines[1].textContent).toContain('100');
    // Disclaimer is shown — VAT will be computed at checkout.
    expect(fixture.nativeElement.querySelector('.cart-summary__vat-note')).not.toBeNull();
  });

  it('renders HT, VAT and TTC rows when amounts are exact (checkout, Stripe Tax has returned)', () => {
    component.summary = {
      items: [{ label: 'SOC', quantity: 1, billingCycle: 'MONTHLY', total: 100 }],
      subtotalHt: 100,
      vatAmount: 20,
      totalTtc: 120,
      currency: 'EUR',
    };
    fixture.detectChanges();

    const lines = fixture.nativeElement.querySelectorAll('.cart-summary__total-line');
    // Three rows: HT subtotal, VAT, grand TTC. No "VAT at checkout" disclaimer.
    expect(lines.length).toBe(3);
    expect(lines[0].textContent).toContain('100');
    expect(lines[1].textContent).toContain('20');
    expect(lines[2].textContent).toContain('120');
    expect(fixture.nativeElement.querySelector('.cart-summary__vat-note:not(.cart-summary__vat-note--reverse)')).toBeNull();
  });

  it('shows the reverse-charge note when the B2B autoliquidation applies', () => {
    component.summary = {
      items: [],
      subtotalHt: 100,
      vatAmount: 0,
      totalTtc: 100,
      currency: 'EUR',
      reverseCharge: true,
    };
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.cart-summary__vat-note--reverse')).not.toBeNull();
  });
});
