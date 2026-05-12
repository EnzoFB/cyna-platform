import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { Observable } from 'rxjs';

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

    // Drain the silent session-restore attempt the service kicks off on boot.
    const restoreReqs = httpMock.match(r => r.url.includes('/auth/refresh'));
    restoreReqs.forEach(r => r.error(new ProgressEvent('error')));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should allow access when authenticated', () => {
    // The login endpoint returns a challenge; tokens land only after OTP verify.
    authService.login('test@cyna.com', 'pass', 'fr').subscribe();
    httpMock.expectOne(r => r.url.includes('/auth/login')).flush({
      success: true,
      data: { challengeId: 'chal_1', expiresInSeconds: 300 },
      timestamp: '',
    });

    authService.verifyOtp('chal_1', '123456').subscribe();
    httpMock.expectOne(r => r.url.includes('/auth/login/verify-otp')).flush({
      success: true,
      data: {
        accessToken: buildMockJwt(),
        refreshToken: 'ref',
        expiresIn: 3600,
        tokenType: 'Bearer',
      },
      timestamp: '',
    });

    const result = TestBed.runInInjectionContext(() => authGuard(buildRoute(), mockState));
    expect(result).toBeTrue();
  });

  it('should redirect to /auth/login when not authenticated', (done) => {
    // When isAuthenticated() is false, the guard attempts a silent session
    // restore via the refresh token. With no token in storage the call fails,
    // and the guard emits a UrlTree pointing at /auth/login. The pipe makes
    // the return an Observable rather than a synchronous value — subscribe to
    // observe the eventual UrlTree.
    const result = TestBed.runInInjectionContext(() => authGuard(buildRoute(), mockState));
    expect(result).toEqual(jasmine.any(Observable));

    (result as Observable<boolean | UrlTree>).subscribe((value) => {
      expect(value).toBeInstanceOf(UrlTree);
      expect((value as UrlTree).toString()).toBe('/auth/login');
      done();
    });
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
