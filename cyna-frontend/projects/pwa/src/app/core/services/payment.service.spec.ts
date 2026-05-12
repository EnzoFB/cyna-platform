import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  FinalizePaymentResponse,
  InitiatePaymentResponse,
  PaymentService
} from './payment.service';
import { ApiResponse } from '../models/api-response.model';
import { environment } from '../../../environments/environment';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
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
    expect(req.request.body).toEqual({ orderId, paymentMethodId });

    const body: ApiResponse<FinalizePaymentResponse> = {
      success: true,
      data: {
        paymentId: 'pay-2',
        orderId,
        lines: [
          { orderLineId: 'l1', subscriptionId: 's1', stripeSubscriptionId: 'sub_a', stripeStatus: 'active' },
          { orderLineId: 'l2', subscriptionId: 's2', stripeSubscriptionId: 'sub_b', stripeStatus: 'incomplete' }
        ]
      },
      timestamp: ''
    };
    req.flush(body);

    expect(received?.lines.length).toBe(2);
    expect(received?.lines[0].stripeStatus).toBe('active');
    expect(received?.lines[1].stripeStatus).toBe('incomplete');
  });
});
