import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRole = route.data?.['role'] as string;

  const user = authService.user();
  if (user && user.role === requiredRole) {
    return true;
  }

  return router.createUrlTree(['/']);
};
