import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRole = route.data?.['role'] as string | undefined;

  if (!requiredRole) {
    return true;
  }

  const user = authService.user();
  if (user && user.roles.includes(requiredRole)) {
    return true;
  }

  return router.createUrlTree(['/login']);
};
