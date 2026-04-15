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
    authService.login('admin@cyna.com', 'pass').subscribe();
    httpMock.expectOne(r => r.url.includes('/auth/login')).flush({
      success: true,
      data: {
        accessToken: buildMockJwt(['ADMIN']),
        refreshToken: 'ref',
        expiresIn: 1,
        tokenType: 'Bearer',
      },
      timestamp: '',
    });

    const result = TestBed.runInInjectionContext(() => roleGuard(buildRoute('ADMIN'), mockState));
    expect(result).toBeTrue();
  });

  it('should redirect when user lacks required role', () => {
    authService.login('user@cyna.com', 'pass').subscribe();
    httpMock.expectOne(r => r.url.includes('/auth/login')).flush({
      success: true,
      data: {
        accessToken: buildMockJwt(['CUSTOMER']),
        refreshToken: 'ref',
        expiresIn: 1,
        tokenType: 'Bearer',
      },
      timestamp: '',
    });

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
