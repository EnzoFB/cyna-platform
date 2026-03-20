import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

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

  it('should send login POST request', () => {
    service.login('admin@test.com', 'password').subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'admin@test.com', password: 'password' });

    req.flush({ data: { accessToken: 'token', refreshToken: 'refresh' } });
  });

  it('should set tokens and user after setTokens()', () => {
    // Create a simple base64-encoded JWT payload
    const payload = btoa(JSON.stringify({ sub: '123', email: 'admin@test.com', role: 'ADMIN' }));
    const fakeJwt = `header.${payload}.signature`;

    service.setTokens({ accessToken: fakeJwt, refreshToken: 'refresh-token' });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.accessToken).toBe(fakeJwt);
    expect(service.user()).toEqual({ id: '123', email: 'admin@test.com', role: 'ADMIN' });
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token');
  });

  it('should clear state and navigate to /login on logout', () => {
    const payload = btoa(JSON.stringify({ sub: '123', email: 'admin@test.com', role: 'ADMIN' }));
    const fakeJwt = `header.${payload}.signature`;
    service.setTokens({ accessToken: fakeJwt, refreshToken: 'refresh-token' });

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.user()).toBeNull();
    expect(service.accessToken).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should handle malformed JWT gracefully in setTokens', () => {
    service.setTokens({ accessToken: 'not-a-jwt', refreshToken: 'refresh' });

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.user()).toBeNull();
  });

  describe('restoreSession', () => {
    it('should return false when no refreshToken in localStorage', (done) => {
      service.restoreSession().subscribe((result) => {
        expect(result).toBeFalse();
        done();
      });
    });

    it('should call refresh endpoint and restore tokens', (done) => {
      localStorage.setItem('refreshToken', 'stored-refresh');
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
      expect(req.request.body).toEqual({ refreshToken: 'stored-refresh' });
      req.flush({ data: { accessToken: newJwt, refreshToken: 'new-refresh' } });
    });

    it('should return false and clear token on refresh failure', (done) => {
      localStorage.setItem('refreshToken', 'expired-refresh');

      service.restoreSession().subscribe((result) => {
        expect(result).toBeFalse();
        expect(localStorage.getItem('refreshToken')).toBeNull();
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      req.flush({ error: 'invalid' }, { status: 401, statusText: 'Unauthorized' });
    });

    it('should return immediately on subsequent calls', (done) => {
      // First call: no token
      service.restoreSession().subscribe(() => {
        // Second call: should return immediately without HTTP
        service.restoreSession().subscribe((result) => {
          expect(result).toBeFalse();
          done();
        });
      });
    });
  });
});
