import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserService } from './user.service';
import { environment } from '../../../environments/environment';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const base = `${environment.apiUrl}/account`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service  = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getProfile GET /account', () => {
    service.getProfile().subscribe();
    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: { id: 'u1', email: 'a@b.com' }, timestamp: '' });
  });

  it('updateProfile PATCH /account/profile with payload', () => {
    service.updateProfile({ firstName: 'Alice', lastName: 'Dupont' }).subscribe();

    const req = httpMock.expectOne(`${base}/profile`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ firstName: 'Alice', lastName: 'Dupont' });
    req.flush({ success: true, data: null, timestamp: '' });
  });

  it('requestEmailChange POST /account/email/request-change', () => {
    service.requestEmailChange({ newEmail: 'b@b.com', lang: 'fr' }).subscribe();

    const req = httpMock.expectOne(`${base}/email/request-change`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ newEmail: 'b@b.com', lang: 'fr' });
    req.flush({ success: true, data: null, timestamp: '' });
  });

  it('changePassword PATCH /account/password', () => {
    service.changePassword('old123!', 'New456!').subscribe();

    const req = httpMock.expectOne(`${base}/password`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ currentPassword: 'old123!', newPassword: 'New456!' });
    req.flush({ success: true, data: null, timestamp: '' });
  });

  it('confirmEmailChange POST /account/email/confirm with token param', () => {
    service.confirmEmailChange('tok-abc').subscribe();

    const req = httpMock.expectOne(r => r.url.includes('/account/email/confirm'));
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('token')).toBe('tok-abc');
    req.flush({ success: true, data: null, timestamp: '' });
  });
});
