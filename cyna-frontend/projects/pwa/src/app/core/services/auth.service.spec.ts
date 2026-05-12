import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { AuthService } from './auth.service';

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

    // Drain initial restore attempt if any
    const restoreReq = httpMock.match(req => req.url.includes('/auth/refresh'));
    restoreReq.forEach(r => r.error(new ProgressEvent('error')));
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

      // Step 2: POST /auth/login/verify-otp returns tokens.
      service.verifyOtp('chal_abc', '123456').subscribe();
      const otpReq = httpMock.expectOne(r => r.url.includes('/auth/login/verify-otp'));
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
      expect(localStorage.getItem('refreshToken')).toBe('refresh-token-abc');
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
          lang: 'fr',
        })
        .subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/register'));
      expect(req.request.method).toBe('POST');
      req.flush(mockAuthResponse);

      expect(service.isAuthenticated()).toBeTrue();
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

      // Then logout
      spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
      service.logout();

      // Drain the logout POST
      const logoutReq = httpMock.match(r => r.url.includes('/auth/logout'));
      logoutReq.forEach(r => r.flush(null));

      expect(service.isAuthenticated()).toBeFalse();
      expect(service.user()).toBeNull();
      expect(service.accessToken).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });

  describe('refreshToken', () => {
    it('should refresh tokens and update user', () => {
      localStorage.setItem('refreshToken', 'old-refresh');

      service.refreshToken().subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'old-refresh' });

      req.flush(mockAuthResponse);

      expect(service.isAuthenticated()).toBeTrue();
      expect(localStorage.getItem('refreshToken')).toBe('refresh-token-abc');
    });

    it('should clear session if refresh fails', () => {
      localStorage.setItem('refreshToken', 'expired-token');

      service.refreshToken().subscribe({ error: () => {} });

      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      req.error(new ProgressEvent('error'), { status: 401 });

      expect(service.isAuthenticated()).toBeFalse();
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });

    it('should error when no refresh token exists', () => {
      let errorThrown = false;
      service.refreshToken().subscribe({
        error: () => {
          errorThrown = true;
        },
      });
      expect(errorThrown).toBeTrue();
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
    it('should restore session from refresh token via restoreSession()', () => {
      localStorage.setItem('refreshToken', 'stored-refresh');

      // The service no longer auto-restores on construction (that responsibility
      // moved to an APP_INITIALIZER) — the test must trigger it explicitly.
      const freshService = TestBed.runInInjectionContext(() => new AuthService());
      freshService.restoreSession().subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/refresh'));
      req.flush(mockAuthResponse);

      expect(freshService.isAuthenticated()).toBeTrue();
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
