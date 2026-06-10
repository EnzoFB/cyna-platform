import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { SubscriptionService, SubscriptionResponse } from './subscription.service';
import { environment } from '../../../environments/environment';

describe('SubscriptionService', () => {
  let service: SubscriptionService;
  let httpMock: HttpTestingController;

  const stub: SubscriptionResponse = {
    id: 'sub-1', userId: 'u1', orderId: 'o1', productId: 'p1',
    productName: 'SOC Standard', productCategory: 'SOC',
    billingCycle: 'MONTHLY', status: 'ACTIVE',
    quantity: 1, unitPrice: 100, currency: 'EUR',
    startAt: '', endAt: '', nextBillingAt: '',
    cancelledAt: null, createdAt: '', updatedAt: '',
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });

    service  = TestBed.inject(SubscriptionService);
    httpMock = TestBed.inject(HttpTestingController);

    httpMock.match(r => r.url.includes('/auth/csrf'))
      .forEach(r => r.flush({ success: true, data: { token: 'tok', headerName: 'X-XSRF-TOKEN' }, timestamp: '' }));
    httpMock.match(r => r.url.includes('/auth/refresh'))
      .forEach(r => r.error(new ProgressEvent('error')));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('list GET /subscriptions with default pagination', () => {
    service.list().subscribe();

    const req = httpMock.expectOne(r => r.url.includes('/subscriptions'));
    expect(req.request.method).toBe('GET');
    expect(req.request.url).toContain('page=0');
    expect(req.request.url).toContain('size=20');
    req.flush({ success: true, data: { items: [stub], totalItems: 1, page: 0, size: 20, totalPages: 1 }, timestamp: '' });
  });

  it('list accepts custom page and size', () => {
    service.list(2, 5).subscribe();

    const req = httpMock.expectOne(r => r.url.includes('/subscriptions'));
    expect(req.request.url).toContain('page=2');
    expect(req.request.url).toContain('size=5');
    req.flush({ success: true, data: { items: [], totalItems: 0, page: 2, size: 5, totalPages: 0 }, timestamp: '' });
  });

  it('cancel POST /subscriptions/:id/cancel returns cancelled subscription', () => {
    const cancelled = { ...stub, status: 'CANCELLED' as const };
    let result: SubscriptionResponse | undefined;
    service.cancel('sub-1').subscribe(r => (result = r));

    const req = httpMock.expectOne(`${environment.apiUrl}/subscriptions/sub-1/cancel`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ success: true, data: cancelled, timestamp: '' });

    expect(result?.status).toBe('CANCELLED');
  });
});
