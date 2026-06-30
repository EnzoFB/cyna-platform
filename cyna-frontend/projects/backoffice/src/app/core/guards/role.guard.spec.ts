import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';

describe('roleGuard', () => {
  let mockAuthService: { user: jasmine.Spy };
  let routerSpy: jasmine.SpyObj<Router>;
  const mockState = {} as RouterStateSnapshot;

  beforeEach(() => {
    mockAuthService = {
      user: jasmine.createSpy('user').and.returnValue(null),
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

  it('should allow access when no role is required', () => {
    const route = { data: {} } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() => roleGuard(route, mockState));

    expect(result).toBeTrue();
  });

  it('should allow access when user has the required role', () => {
    mockAuthService.user.and.returnValue({ id: '1', email: 'admin@test.com', roles: ['ADMIN'] });
    const route = { data: { role: 'ADMIN' } } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() => roleGuard(route, mockState));

    expect(result).toBeTrue();
  });

  it('should redirect to /login when user does not have the required role', () => {
    mockAuthService.user.and.returnValue({ id: '1', email: 'user@test.com', roles: ['CUSTOMER'] });
    const route = { data: { role: 'ADMIN' } } as unknown as ActivatedRouteSnapshot;

    TestBed.runInInjectionContext(() => roleGuard(route, mockState));

    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
  });

  it('should redirect to /login when user is null and role is required', () => {
    const route = { data: { role: 'ADMIN' } } as unknown as ActivatedRouteSnapshot;

    TestBed.runInInjectionContext(() => roleGuard(route, mockState));

    expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/login']);
  });
});
