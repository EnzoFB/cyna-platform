import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { OrderService, OrderResponse } from './order.service';
import { environment } from '../../../environments/environment';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });

    service  = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);

    // Drain AuthService bootstrap calls (CSRF + session restore).
    httpMock.match(r => r.url.includes('/auth/csrf'))
      .forEach(r => r.flush({ success: true, data: { token: 'tok', headerName: 'X-XSRF-TOKEN' }, timestamp: '' }));
    httpMock.match(r => r.url.includes('/auth/refresh'))
      .forEach(r => r.error(new ProgressEvent('error')));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('createOrder POST /orders with lines and returns the order id', () => {
    const lines = [
      { productId: 'prod-1', billingCycle: 'MONTHLY' as const, quantity: 1 },
    ];
    let orderId: string | undefined;
    service.createOrder(lines).subscribe(id => (orderId = id));

    const req = httpMock.expectOne(`${environment.apiUrl}/orders`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ lines });
    req.flush({ success: true, data: 'order-abc', timestamp: '' });

    expect(orderId).toBe('order-abc');
  });

  it('getOrder GET /orders/:id returns the order response', () => {
    let result: OrderResponse | undefined;
    service.getOrder('order-abc').subscribe(o => (result = o));

    const req = httpMock.expectOne(`${environment.apiUrl}/orders/order-abc`);
    expect(req.request.method).toBe('GET');
    const stub: OrderResponse = {
      id: 'order-abc', userId: 'u1', status: 'PENDING',
      subtotalHt: 100,
      currency: 'EUR', createdAt: '', lines: [],
    };
    req.flush({ success: true, data: stub, timestamp: '' });

    expect(result?.id).toBe('order-abc');
    expect(result?.subtotalHt).toBe(100);
  });
});
