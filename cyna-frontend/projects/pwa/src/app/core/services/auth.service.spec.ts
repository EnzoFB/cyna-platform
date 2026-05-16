import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * Refresh-token storage moved off localStorage. The backend issues an
 * HttpOnly cookie that the JS layer can't see; HttpTestingController
 * can't observe that either, so tests focus on what the AuthService
 * DOES from the JS side: which HTTP it fires, what body it carries,
 * how it mutates the in-memory access-token signal.
 */
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  const mockAuthResponse = {
    success: true,
    data: {
      accessToken: buildMockJwt({ sub: 'user-123', email: 'test@cyna.com', roles: ['CUSTOMER'] }),
      refreshToken: 'refresh-token-abc',
      expiresIn: 1,
      tokenType: 'Bearer',
    },
    timestamp: new Date().toISOString(),
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should not be authenticated initially', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.user()).toBeNull();
    expect(service.accessToken).toBeNull();
  });

  describe('login', () => {
    it('should issue a challenge on login but stay unauthenticated until OTP verified', () => {
      // Step 1: POST /auth/login returns a challenge, NOT tokens.
      service.login('test@cyna.com', 'password123', 'fr').subscribe();

      const loginReq = httpMock.expectOne(r => r.url.includes('/auth/login'));
      expect(loginReq.request.method).toBe('POST');
      expect(loginReq.request.body).toEqual({ email: 'test@cyna.com', password: 'password123', lang: 'fr' });
      loginReq.flush({
        success: true,
        data: { challengeId: 'chal_abc', expiresInSeconds: 300 },
        timestamp: '',
      });

      // After the credentials step the user is NOT yet authenticated — only the
      // OTP verify call below sets tokens.
      expect(service.isAuthenticated()).toBeFalse();

      // Step 2: POST /auth/login/verify-otp returns tokens. With cookies, the
      // request is sent with credentials and the refresh token never lands
      // in localStorage; only the in-memory access-token signal flips.
      service.verifyOtp('chal_abc', '123456').subscribe();
      const otpReq = httpMock.expectOne(r => r.url.includes('/auth/login/verify-otp'));
      expect(otpReq.request.withCredentials).withContext('OTP verify must send the cookie').toBeTrue();
      otpReq.flush(mockAuthResponse);

      expect(service.isAuthenticated()).toBeTrue();
      // The JWT payload doesn't carry firstName/lastName so those decode to
      // undefined on the AuthUser; we only assert on the identity fields.
      expect(service.user()).toEqual(jasmine.objectContaining({
        id: 'user-123',
        email: 'test@cyna.com',
        roles: ['CUSTOMER'],
      }));
      expect(service.accessToken).toBeTruthy();
      // Refresh token NEVER touches JS-readable storage anymore.
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });

    it('should not authenticate on login failure', () => {
      service.login('test@cyna.com', 'wrong', 'fr').subscribe({ error: () => {} });

      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      req.error(new ProgressEvent('error'), { status: 401 });

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.user()).toBeNull();
    });
  });

  describe('register', () => {
    it('should authenticate after registration', () => {
      service
        .register({
          email: 'new@cyna.com',
          password: 'Secure123!',
          firstName: 'Test',
          lastName: 'User',
          company: 'Acme Corp',
          lang: 'fr',
          acceptTerms: true,
        })
        .subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/register'));
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).withContext('register must send the cookie').toBeTrue();
      req.flush(mockAuthResponse);

      expect(service.isAuthenticated()).toBeTrue();
    });
  });

  describe('password reset', () => {
    it('requestPasswordReset POSTs email + lang to /auth/forgot-password', () => {
      service.requestPasswordReset('user@cyna.com', 'fr').subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/forgot-password'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'user@cyna.com', lang: 'fr' });
      req.flush({ success: true, data: null });
    });

    it('resetPassword POSTs token + newPassword to /auth/reset-password', () => {
      service.resetPassword('tok-123', 'Secure123!').subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/reset-password'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ token: 'tok-123', newPassword: 'Secure123!' });
      req.flush({ success: true, data: null });
    });
  });

  describe('logout', () => {
    it('should clear session on logout', () => {
      // First authenticate via the 2-step OTP flow.
      service.login('test@cyna.com', 'pass', 'fr').subscribe();
      httpMock.expectOne(r => r.url.includes('/auth/login')).flush({
        success: true,
        data: { challengeId: 'chal_logout', expiresInSeconds: 300 },
        timestamp: '',
      });
      service.verifyOtp('chal_logout', '123456').subscribe();
      httpMock.expectOne(r => r.url.includes('/auth/login/verify-otp')).flush(mockAuthResponse);
      expect(service.isAuthenticated()).toBeTrue();

      // Then logout. Empty body — the cookie carries the refresh token.
      spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
      service.logout();

      const logoutReq = httpMock.expectOne(r => r.url.includes('/auth/logout'));
      expect(logoutReq.request.body).toEqual({});
      expect(logoutReq.request.withCredentials).toBeTrue();
      logoutReq.flush(null);

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.user()).toBeNull();
      expect(service.accessToken).toBeNull();
    });
  });

  describe('refreshToken', () => {
    it('should refresh with an empty body when no legacy localStorage token exists', () => {
      service.refreshToken().subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      expect(req.request.withCredentials).withContext('refresh must send the cookie').toBeTrue();
      req.flush(mockAuthResponse);

      expect(service.isAuthenticated()).toBeTrue();
      // Refresh token stays in the cookie (invisible to JS). JS-readable
      // storage stays clean.
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });

    it('should clear session if refresh fails', () => {
      service.refreshToken().subscribe({ error: () => {} });

      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      req.error(new ProgressEvent('error'), { status: 401 });

      expect(service.isAuthenticated()).toBeFalse();
    });

    it('should replay a legacy localStorage token once, then delete it', () => {
      // Pre-migration users still have a refresh token in localStorage left
      // over from the old scheme. The bootstrap refresh replays it in the
      // body so the backend rotates them onto the cookie without forcing a
      // re-login. The localStorage entry must be wiped exactly once.
      localStorage.setItem('refreshToken', 'legacy-token');

      service.refreshToken().subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      expect(req.request.body).toEqual({ refreshToken: 'legacy-token' });
      req.flush(mockAuthResponse);

      expect(localStorage.getItem('refreshToken'))
          .withContext('legacy entry must be wiped after the first round-trip')
          .toBeNull();

      // Second refresh call: cookie-only, empty body.
      service.refreshToken().subscribe();
      const second = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      expect(second.request.body).toEqual({});
      second.flush(mockAuthResponse);
    });

    it('should also wipe the legacy entry even when the refresh fails', () => {
      localStorage.setItem('refreshToken', 'legacy-token');

      service.refreshToken().subscribe({ error: () => {} });
      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      req.error(new ProgressEvent('error'), { status: 401 });

      // Don't keep replaying a token the backend has already rejected.
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });

  describe('checkEmail', () => {
    it('should check email availability', () => {
      service.checkEmail('test@cyna.com').subscribe(res => {
        expect(res.data).toBeTrue();
      });

      const req = httpMock.expectOne(r => r.url.includes('/account/check-email'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('email')).toBe('test@cyna.com');
      req.flush({ success: true, data: true, timestamp: new Date().toISOString() });
    });
  });

  describe('session restore', () => {
    it('should restore session via /auth/refresh on a fresh boot', () => {
      // restoreSession() is wired into provideAppInitializer at app boot,
      // so the test triggers it explicitly on a fresh service.
      const freshService = TestBed.runInInjectionContext(() => new AuthService());
      freshService.restoreSession().subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      expect(req.request.withCredentials).toBeTrue();
      req.flush(mockAuthResponse);

      expect(freshService.isAuthenticated()).toBeTrue();
    });

    it('should resolve false when no cookie is set on the browser', () => {
      const freshService = TestBed.runInInjectionContext(() => new AuthService());
      let restored: boolean | null = null;
      freshService.restoreSession().subscribe(r => (restored = r));

      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      req.error(new ProgressEvent('error'), { status: 401 });

      expect(restored).toBeFalse();
      expect(freshService.isAuthenticated()).toBeFalse();
    });
  });

  describe('refresh deduplication', () => {
    // Regression test for the F5-on-/account bug (PR #183): every concurrent
    // caller must share one /auth/refresh round-trip. With APP_INITIALIZER
    // now blocking on restoreSession, this race is mostly impossible at
    // boot, but it can still happen mid-session (multiple parallel HTTPs
    // hit 401 at the same time and all funnel through the interceptor).
    it('should issue a single POST /auth/refresh for parallel refreshToken() calls', () => {
      let aCompleted = false;
      let bCompleted = false;
      service.refreshToken().subscribe(() => (aCompleted = true));
      service.refreshToken().subscribe(() => (bCompleted = true));

      const reqs = httpMock.match(r => r.url.includes('/auth/refresh'));
      expect(reqs.length).toBe(1);
      reqs[0].flush(mockAuthResponse);

      expect(aCompleted).toBeTrue();
      expect(bCompleted).toBeTrue();
    });

    it('should allow a fresh refresh after the previous one completed', () => {
      service.refreshToken().subscribe();
      httpMock.expectOne(r => r.url.includes('/auth/refresh')).flush(mockAuthResponse);

      service.refreshToken().subscribe();
      const second = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      expect(second).toBeTruthy();
      second.flush(mockAuthResponse);
    });
  });
});

function buildMockJwt(payload: { sub: string; email: string; roles: string[] }): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(
    JSON.stringify({
      ...payload,
      iss: 'cyna-platform',
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
  );
  const signature = btoa('fake-signature');
  return `${header}.${body}.${signature}`;
}
