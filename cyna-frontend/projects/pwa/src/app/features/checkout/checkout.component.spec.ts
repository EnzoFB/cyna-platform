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
});
