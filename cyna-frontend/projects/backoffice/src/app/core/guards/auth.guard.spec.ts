import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let mockAuthService: { isAuthenticated: jasmine.Spy; restoreSession: jasmine.Spy };
  let routerSpy: jasmine.SpyObj<Router>;
  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = {} as RouterStateSnapshot;

  beforeEach(() => {
    mockAuthService = {
      isAuthenticated: jasmine.createSpy('isAuthenticated').and.returnValue(false),
      restoreSession: jasmine.createSpy('restoreSession').and.returnValue(of(false)),
    };
    routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);
    routerSpy.createUrlTree.and.returnValue({} as any);

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: routerSpy },
      ],
    });
  });

  it('should allow access when already authenticated', () => {
    mockAuthService.isAuthenticated.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

    expect(result).toBeTrue();
    expect(mockAuthService.restoreSession).not.toHaveBeenCalled();
  });

  it('should attempt session restore when not authenticated', (done) => {
    mockAuthService.restoreSession.and.returnValue(of(true));

    const result$ = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

    (result$ as any).subscribe((result: boolean) => {
      expect(result).toBeTrue();
      expect(mockAuthService.restoreSession).toHaveBeenCalled();
      done();
    });
  });

  it('should redirect to /login when session restore fails', (done) => {
    mockAuthService.restoreSession.and.returnValue(of(false));

    const result$ = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

    (result$ as any).subscribe(() => {
      expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
      done();
    });
  });
});
