import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRole = route.data?.['role'] as string | undefined;

  if (authService.isAuthenticated()) {
    return checkRole(authService, router, requiredRole);
  }

  return authService.restoreSession().pipe(
    map((restored) => {
      if (!restored) {
        return router.createUrlTree(['/login']);
      }
      return checkRole(authService, router, requiredRole);
    }),
  );
};

function checkRole(
  authService: AuthService,
  router: Router,
  requiredRole: string | undefined,
): boolean | UrlTree {
  if (!requiredRole) {
    return true;
  }
  const user = authService.user();
  if (user && user.roles.includes(requiredRole)) {
    return true;
  }
  return router.createUrlTree(['/login']);
}
