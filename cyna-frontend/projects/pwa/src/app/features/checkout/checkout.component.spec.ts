import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';

import { CheckoutComponent } from './checkout.component';

describe('Checkout', () => {
  let component: CheckoutComponent;
  let fixture: ComponentFixture<CheckoutComponent>;

  beforeEach(async () => {
    localStorage.removeItem('cyna_pwa_cart');
    localStorage.removeItem('cyna_pwa_guest_token');

    await TestBed.configureTestingModule({
      imports: [CheckoutComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslateService(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CheckoutComponent);
    component = fixture.componentInstance;
    // detectChanges() would trigger ngAfterViewInit which loads Stripe.js from the
    // network — we don't want that in a unit test. Skip it; the smoke test is
    // about the component graph being constructible.
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // buildVatNumber() resolves the B2B VAT number sent to /payments/finalize.
  // It is the bridge between what the user types and the reverse-charge logic.
  describe('buildVatNumber (B2B VAT resolution sent to finalize)', () => {
    const callBuildVat = (): string | null => (component as never as { buildVatNumber(): string | null }).buildVatNumber();

    it('returns the trimmed inline VAT number in "new address" mode', () => {
      component.addressMode.set('new');
      component.form.controls.billing.patchValue({ vatNumber: '  FR12345678901  ' });

      expect(callBuildVat()).toBe('FR12345678901');
    });

    it('returns null when the inline VAT number is blank (B2C)', () => {
      component.addressMode.set('new');
      component.form.controls.billing.patchValue({ vatNumber: '   ' });

      expect(callBuildVat()).toBeNull();
    });

    it('uses the selected saved address VAT number in "saved" mode', () => {
      component.addressMode.set('saved');
      component.selectedAddress.set({ vatNumber: 'DE123456789' } as never);
      // Even if the inline form has a value, saved mode must win.
      component.form.controls.billing.patchValue({ vatNumber: 'FR999' });

      expect(callBuildVat()).toBe('DE123456789');
    });

    it('returns null when the saved address carries no VAT number', () => {
      component.addressMode.set('saved');
      component.selectedAddress.set({ vatNumber: null } as never);

      expect(callBuildVat()).toBeNull();
    });
  });
});
