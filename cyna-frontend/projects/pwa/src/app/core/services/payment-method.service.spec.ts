import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PaymentMethodService } from './payment-method.service';
import { SavedPaymentMethod } from '../models/saved-payment-method.model';
import { environment } from '../../../environments/environment';

describe('PaymentMethodService', () => {
  let service: PaymentMethodService;
  let httpMock: HttpTestingController;

  const base = `${environment.apiUrl}/account/payment-methods`;

  const stub: SavedPaymentMethod = {
    id: 'pm-1',
    stripePaymentMethodId: 'pm_stripe_1',
    brand: 'visa',
    last4: '4242',
    expMonth: '12',
    expYear: '2027',
    holderName: null,
    isDefault: true,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service  = TestBed.inject(PaymentMethodService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getAll GET /account/payment-methods and returns array', () => {
    let result: SavedPaymentMethod[] | undefined;
    service.getAll().subscribe(r => (result = r));

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [stub], timestamp: '' });

    expect(result?.length).toBe(1);
    expect(result?.[0].last4).toBe('4242');
  });

  it('getAll returns empty array when data is null', () => {
    let result: SavedPaymentMethod[] | undefined;
    service.getAll().subscribe(r => (result = r));

    httpMock.expectOne(base).flush({ success: true, data: null, timestamp: '' });

    expect(result).toEqual([]);
  });

  it('save POST /account/payment-methods with stripePaymentMethodId', () => {
    service.save('pm_stripe_abc').subscribe();

    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ stripePaymentMethodId: 'pm_stripe_abc' });
    req.flush({ success: true, data: null, timestamp: '' });
  });
});
