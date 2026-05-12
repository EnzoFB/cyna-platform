import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';

import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let authService: AuthService;
  let httpMock: HttpTestingController;

  const mockState = {} as RouterStateSnapshot;

  function buildRoute(role: string): ActivatedRouteSnapshot {
    return { data: { role } } as unknown as ActivatedRouteSnapshot;
  }

  /**
   * Drives the 2-step OTP login until the AuthService has issued tokens. After
   * the refactor, plain {@code login()} only returns a challenge — actual auth
   * state is only set by {@code verifyOtp()}.
   */
  function authenticateWith(roles: string[]): void {
    authService.login('user@cyna.com', 'pass', 'fr').subscribe();
    httpMock.expectOne(r => r.url.includes('/auth/login')).flush({
      success: true,
      data: { challengeId: 'chal_x', expiresInSeconds: 300 },
      timestamp: '',
    });

    authService.verifyOtp('chal_x', '123456').subscribe();
    httpMock.expectOne(r => r.url.includes('/auth/login/verify-otp')).flush({
      success: true,
      data: {
        accessToken: buildMockJwt(roles),
        refreshToken: 'ref',
        expiresIn: 3600,
        tokenType: 'Bearer',
      },
      timestamp: '',
    });
  }

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });

    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    const restoreReqs = httpMock.match(r => r.url.includes('/auth/refresh'));
    restoreReqs.forEach(r => r.error(new ProgressEvent('error')));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should allow access when user has required role', () => {
    authenticateWith(['ADMIN']);

    const result = TestBed.runInInjectionContext(() => roleGuard(buildRoute('ADMIN'), mockState));
    expect(result).toBeTrue();
  });

  it('should redirect when user lacks required role', () => {
    authenticateWith(['CUSTOMER']);

    const result = TestBed.runInInjectionContext(() => roleGuard(buildRoute('ADMIN'), mockState));
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('should redirect when user is not authenticated', () => {
    const result = TestBed.runInInjectionContext(() => roleGuard(buildRoute('ADMIN'), mockState));
    expect(result).toBeInstanceOf(UrlTree);
  });
});

function buildMockJwt(roles: string[] = ['CUSTOMER']): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(
    JSON.stringify({
      sub: 'user-1',
      email: 'test@cyna.com',
      roles,
      iss: 'cyna-platform',
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
  );
  return `${header}.${body}.${btoa('sig')}`;
}
