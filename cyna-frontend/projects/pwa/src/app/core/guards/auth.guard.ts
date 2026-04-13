import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import {ToastService} from "../services/toast.service";
import {TranslateService} from "@ngx-translate/core";

export const authGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const toastService = inject(ToastService);
  const translateService = inject(TranslateService)

  if (authService.isAuthenticated()) {
    return true;
  }

  if (route.data?.['showAuthToast']) {
    toastService.showWarning(translateService.instant('cartPage.toastAuthRequired'));
  }

  return router.createUrlTree(['/auth/login']);
};
