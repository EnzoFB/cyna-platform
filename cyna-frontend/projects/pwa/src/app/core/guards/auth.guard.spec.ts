import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authService: AuthService;
  let httpMock: HttpTestingController;

  const mockState = {} as RouterStateSnapshot;

  function buildRoute(data: Record<string, unknown> = {}): ActivatedRouteSnapshot {
    return { data } as unknown as ActivatedRouteSnapshot;
  }

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });

    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);

    // Drain restore attempt
    const restoreReqs = httpMock.match(r => r.url.includes('/auth/refresh'));
    restoreReqs.forEach(r => r.error(new ProgressEvent('error')));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should allow access when authenticated', () => {
    authService.login('test@cyna.com', 'pass').subscribe();
    httpMock.expectOne(r => r.url.includes('/auth/login')).flush({
      success: true,
      data: {
        accessToken: buildMockJwt(),
        refreshToken: 'ref',
        expiresIn: 1,
        tokenType: 'Bearer',
      },
      timestamp: '',
    });

    const result = TestBed.runInInjectionContext(() => authGuard(buildRoute(), mockState));
    expect(result).toBeTrue();
  });

  it('should redirect to /auth/login when not authenticated', () => {
    const result = TestBed.runInInjectionContext(() => authGuard(buildRoute(), mockState));
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/auth/login');
  });
});

function buildMockJwt(): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(
    JSON.stringify({
      sub: 'user-1',
      email: 'test@cyna.com',
      roles: ['CUSTOMER'],
      iss: 'cyna-platform',
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600,
    })
  );
  return `${header}.${body}.${btoa('sig')}`;
}
