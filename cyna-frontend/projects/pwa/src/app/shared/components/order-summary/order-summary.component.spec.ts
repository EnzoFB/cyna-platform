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
      vatAmount: 0,
      totalTtc: 0,
      currency: 'EUR',
    };
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the HT subtotal, VAT amount and TTC total from the summary', () => {
    component.summary = {
      items: [{ label: 'SOC', quantity: 1, billingCycle: 'MONTHLY', total: 100 }],
      subtotalHt: 100,
      vatAmount: 20,
      totalTtc: 120,
      currency: 'EUR',
    };
    fixture.detectChanges();

    const lines = fixture.nativeElement.querySelectorAll('.cart-summary__total-line');
    // Three rows: subtotal (HT), VAT, grand total (TTC).
    expect(lines.length).toBe(3);
    expect(lines[0].textContent).toContain('100');
    expect(lines[1].textContent).toContain('20');
    expect(lines[2].textContent).toContain('120');
  });

  it('hides the VAT note by default', () => {
    component.summary = { items: [], subtotalHt: 0, vatAmount: 0, totalTtc: 0, currency: 'EUR' };
    component.options = {};
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.cart-summary__vat-note')).toBeNull();
  });

  it('shows the VAT note only when options.vatNote is enabled (checkout)', () => {
    component.summary = { items: [], subtotalHt: 0, vatAmount: 0, totalTtc: 0, currency: 'EUR' };
    component.options = { vatNote: true };
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.cart-summary__vat-note')).not.toBeNull();
  });

  it('hides the estimate note once amounts are exact (Stripe Tax)', () => {
    component.summary = {
      items: [], subtotalHt: 100, vatAmount: 20, totalTtc: 120, currency: 'EUR',
      vatExact: true, reverseCharge: false,
    };
    component.options = { vatNote: true };
    fixture.detectChanges();

    // Exact amounts → no "estimated" disclaimer.
    expect(fixture.nativeElement.querySelector('.cart-summary__vat-note')).toBeNull();
  });

  it('shows the reverse-charge note when the B2B autoliquidation applies', () => {
    component.summary = {
      items: [], subtotalHt: 100, vatAmount: 0, totalTtc: 100, currency: 'EUR',
      vatExact: true, reverseCharge: true,
    };
    component.options = { vatNote: true };
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.cart-summary__vat-note--reverse')).not.toBeNull();
  });
});
