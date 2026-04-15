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
    it('should authenticate and decode JWT on login', () => {
      service.login('test@cyna.com', 'password123').subscribe();

      const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'test@cyna.com', password: 'password123' });

      req.flush(mockAuthResponse);

      expect(service.isAuthenticated()).toBeTrue();
      expect(service.user()).toEqual({
        id: 'user-123',
        email: 'test@cyna.com',
        roles: ['CUSTOMER'],
      });
      expect(service.accessToken).toBeTruthy();
      expect(localStorage.getItem('refreshToken')).toBe('refresh-token-abc');
    });

    it('should not authenticate on login failure', () => {
      service.login('test@cyna.com', 'wrong').subscribe({ error: () => {} });

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
      // First login
      service.login('test@cyna.com', 'pass').subscribe();
      httpMock.expectOne(r => r.url.includes('/auth/login')).flush(mockAuthResponse);
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
    it('should restore session from refresh token on init', () => {
      localStorage.setItem('refreshToken', 'stored-refresh');

      // Create a new service instance inside injection context
      const freshService = TestBed.runInInjectionContext(() => new AuthService());

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
