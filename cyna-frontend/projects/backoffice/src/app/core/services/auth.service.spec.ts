import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

/**
 * Refresh-token storage moved off localStorage. The backend issues an
 * HttpOnly cookie that the JS layer can't see; HttpTestingController
 * can't observe it either, so tests focus on what AuthService does from
 * the JS side: which HTTP it fires, what body it carries, and how it
 * mutates the in-memory access-token state.
 */
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
        AuthService,
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start as not authenticated', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.user()).toBeNull();
    expect(service.accessToken).toBeNull();
  });

  it('should send login POST request to the admin endpoint and receive a challenge', () => {
    service.login('admin@test.com', 'password').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/admin/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@test.com', password: 'password' });

    // The admin login no longer returns tokens directly — it kicks off the OTP
    // step. AuthService.verifyOtp() is what eventually issues the tokens.
    req.flush({
      success: true,
      data: { challengeId: 'chal_xyz', expiresInSeconds: 300 },
      timestamp: '2026-05-11T00:00:00Z',
    });
  });

  it('should set tokens and user after setTokens()', () => {
    const payload = btoa(JSON.stringify({ sub: '123', email: 'admin@test.com', role: 'ADMIN' }));
    const fakeJwt = `header.${payload}.signature`;

    service.setTokens({ accessToken: fakeJwt, refreshToken: 'refresh-token' });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.accessToken).toBe(fakeJwt);
    expect(service.user()).toEqual({ id: '123', email: 'admin@test.com', roles: ['ADMIN'] });
    // Refresh token lives in the HttpOnly cookie now — NOT in localStorage.
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it('should clear state and navigate to /login on logout', () => {
    const payload = btoa(JSON.stringify({ sub: '123', email: 'admin@test.com', role: 'ADMIN' }));
    const fakeJwt = `header.${payload}.signature`;
    service.setTokens({ accessToken: fakeJwt, refreshToken: 'refresh-token' });

    service.logout();

    // Empty body — the backend resolves the refresh token from the cookie.
    const logoutReq = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
    expect(logoutReq.request.method).toBe('POST');
    expect(logoutReq.request.body).toEqual({});
    expect(logoutReq.request.withCredentials).toBeTrue();
    logoutReq.flush(null);

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.user()).toBeNull();
    expect(service.accessToken).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should handle malformed JWT gracefully in setTokens', () => {
    service.setTokens({ accessToken: 'not-a-jwt', refreshToken: 'refresh' });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.user()).toBeNull();
  });

  describe('restoreSession', () => {
    it('should call /auth/refresh on a fresh boot and authenticate on success', (done) => {
      const payload = btoa(JSON.stringify({ sub: '456', email: 'restored@test.com', role: 'ADMIN' }));
      const newJwt = `header.${payload}.signature`;

      service.restoreSession().subscribe((result) => {
        expect(result).toBeTrue();
        expect(service.isAuthenticated()).toBeTrue();
        expect(service.user()?.email).toBe('restored@test.com');
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      expect(req.request.method).toBe('POST');
      // No localStorage token → empty body, cookie auth.
      expect(req.request.body).toEqual({});
      expect(req.request.withCredentials).toBeTrue();
      req.flush({ data: { accessToken: newJwt, refreshToken: 'new-refresh' } });
    });

    it('should return false on refresh failure (no cookie set)', (done) => {
      service.restoreSession().subscribe((result) => {
        expect(result).toBeFalse();
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      req.flush({ error: 'invalid' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('should replay a legacy localStorage token once then delete it', () => {
      localStorage.setItem('refreshToken', 'legacy-refresh');
      const payload = btoa(JSON.stringify({ sub: '789', email: 'legacy@test.com', role: 'ADMIN' }));
      const newJwt = `header.${payload}.signature`;

      service.restoreSession().subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      expect(req.request.body).toEqual({ refreshToken: 'legacy-refresh' });
      req.flush({ data: { accessToken: newJwt, refreshToken: 'new-refresh' } });

      // finalize fires AFTER the response is consumed, so we check on the
      // outside of the subscribe callback — not inside it, where finalize
      // hasn't run yet.
      expect(localStorage.getItem('refreshToken'))
        .withContext('legacy entry must be wiped after the first round-trip')
        .toBeNull();
    });

    it('should short-circuit on subsequent calls without issuing a new HTTP', (done) => {
      service.restoreSession().subscribe(() => {
        service.restoreSession().subscribe((result) => {
          expect(result).toBeFalse();
          done();
        });
      });

      // Only ONE refresh call total.
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      req.flush({ error: 'invalid' }, { status: 401, statusText: 'Unauthorized' });
    });
  });
});
