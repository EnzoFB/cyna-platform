import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);

    // Drain restore attempt
    const restoreReqs = httpMock.match(r => r.url.includes('/auth/refresh'));
    restoreReqs.forEach(r => r.error(new ProgressEvent('error')));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should add Authorization header when token is available', () => {
    // Manually set a token via login
    authService.login('test@cyna.com', 'pass').subscribe();
    const loginReq = httpMock.expectOne(r => r.url.includes('/auth/login'));
    loginReq.flush({
      success: true,
      data: {
        accessToken: buildMockJwt(),
        refreshToken: 'ref',
        expiresIn: 1,
        tokenType: 'Bearer',
      },
      timestamp: '',
    });

    // Now make a regular request
    httpClient.get('/api/v1/products').subscribe();
    const req = httpMock.expectOne('/api/v1/products');
    expect(req.request.headers.get('Authorization')).toMatch(/^Bearer /);
    req.flush([]);
  });

  it('should not add Authorization header when no token', () => {
    httpClient.get('/api/v1/products').subscribe();
    const req = httpMock.expectOne('/api/v1/products');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush([]);
  });

  it('should skip interceptor for auth endpoints', () => {
    httpClient.post('/api/v1/auth/login', {}).subscribe();
    const req = httpMock.expectOne(r => r.url.includes('/auth/login'));
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
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
