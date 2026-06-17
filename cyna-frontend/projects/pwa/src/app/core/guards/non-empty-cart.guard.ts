import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { CartService } from '../services/cart.service';

export const nonEmptyCartGuard: CanActivateFn = () => {
  const cartService = inject(CartService);
  const router = inject(Router);

  if (!cartService.isEmpty()) {
    return true;
  }

  return router.createUrlTree(['/cart']);
};
