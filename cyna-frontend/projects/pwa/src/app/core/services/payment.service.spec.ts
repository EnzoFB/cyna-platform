import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  FinalizePaymentResponse,
  InitiatePaymentResponse,
  PaymentService,
  TaxPreviewResponse
} from './payment.service';
import { ApiResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';
import { TranslateModule } from '@ngx-translate/core';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POST /payments/initiate carries orderId and returns the SetupIntent client_secret', () => {
    const orderId = 'order-1';
    let received: InitiatePaymentResponse | undefined;

    service.initiatePayment(orderId).subscribe(r => { received = r; });

    const req = httpMock.expectOne(`${environment.apiUrl}/payments/initiate`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ orderId });

    const body: ApiResponse<InitiatePaymentResponse> = {
      success: true,
      data: {
        paymentId: 'pay-1',
        orderId,
        setupIntentClientSecret: 'seti_secret_abc',
        amount: 120,
        currency: 'EUR'
      },
      timestamp: ''
    };
    req.flush(body);

    expect(received?.setupIntentClientSecret).toBe('seti_secret_abc');
    expect(received?.amount).toBe(120);
  });

  it('POST /payments/finalize sends paymentMethodId and exposes per-line results', () => {
    const orderId = 'order-2';
    const paymentMethodId = 'pm_card_visa';
    let received: FinalizePaymentResponse | undefined;

    service.finalizePayment(orderId, paymentMethodId).subscribe(r => { received = r; });

    const req = httpMock.expectOne(`${environment.apiUrl}/payments/finalize`);
    expect(req.request.method).toBe('POST');
    // No VAT number supplied → vatNumber must be explicitly null (B2C path).
    expect(req.request.body).toEqual({ orderId, paymentMethodId, vatNumber: null, lang: 'fr' });

    const body: ApiResponse<FinalizePaymentResponse> = {
      success: true,
      data: {
        paymentId: 'pay-2',
        orderId,
        requiresAction: false,
        lines: [
          { orderLineId: 'l1', subscriptionId: 's1', stripeSubscriptionId: 'sub_a', stripeStatus: 'active' },
          { orderLineId: 'l2', subscriptionId: 's2', stripeSubscriptionId: 'sub_b', stripeStatus: 'incomplete' }
        ],
        pendingActions: []
      },
      timestamp: ''
    };
    req.flush(body);

    expect(received?.lines.length).toBe(2);
    expect(received?.lines[0].stripeStatus).toBe('active');
    expect(received?.lines[1].stripeStatus).toBe('incomplete');
  });

  it('POST /payments/finalize forwards a B2B VAT number when supplied', () => {
    const orderId = 'order-3';
    const paymentMethodId = 'pm_card_visa';

    service.finalizePayment(orderId, paymentMethodId, 'DE123456789').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/payments/finalize`);
    // The VAT number is what lets the backend trigger the intra-EU reverse charge.
    expect(req.request.body).toEqual({ orderId, paymentMethodId, vatNumber: 'DE123456789', lang: 'fr' });
    req.flush({ success: true, data: { paymentId: 'p', orderId, lines: [] }, timestamp: '' });
  });

  it('POST /payments/finalize normalizes an empty VAT number to null (B2C)', () => {
    const orderId = 'order-4';
    const paymentMethodId = 'pm_card_visa';

    service.finalizePayment(orderId, paymentMethodId, '').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/payments/finalize`);
    expect(req.request.body).toEqual({ orderId, paymentMethodId, vatNumber: null, lang: 'fr' });
    req.flush({ success: true, data: { paymentId: 'p', orderId, lines: [] }, timestamp: '' });
  });

  it('POST /payments/tax-preview sends the cart lines + location and returns exact VAT', () => {
    let received: TaxPreviewResponse | undefined;

    service.previewTax({
      currency: 'EUR',
      countryCode: 'DE',
      vatNumber: 'DE123456789',
      lines: [{ productId: 'p1', billingCycle: 'MONTHLY', quantity: 2 }],
    }).subscribe(r => { received = r; });

    const req = httpMock.expectOne(`${environment.apiUrl}/payments/tax-preview`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.countryCode).toBe('DE');
    expect(req.request.body.vatNumber).toBe('DE123456789');
    expect(req.request.body.lines.length).toBe(1);

    const body: ApiResponse<TaxPreviewResponse> = {
      success: true,
      data: {
        exact: true,
        subtotalHt: 200,
        vatAmount: 0,
        totalTtc: 200,
        currency: 'EUR',
        reverseCharge: true,
      },
      timestamp: '',
    };
    req.flush(body);

    expect(received?.exact).toBeTrue();
    expect(received?.reverseCharge).toBeTrue();
    expect(received?.totalTtc).toBe(200);
  });
});
