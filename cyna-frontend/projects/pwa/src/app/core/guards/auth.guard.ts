import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { map } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { TranslateService } from '@ngx-translate/core';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const toastService = inject(ToastService);
  const translateService = inject(TranslateService);

  if (authService.isAuthenticated()) {
    return true;
  }

  return authService.restoreSession().pipe(
    map((restored) => {
      if (restored) {
        return true;
      }

      if (route.data?.['showAuthToast']) {
        const toastKey = (route.data?.['toastKey'] as string);
        toastService.showWarning(translateService.instant(toastKey));
      }

      // Remember the page the user was trying to reach so the login flow can
      // send them back there once authenticated, instead of dropping them on
      // the home page.
      return router.createUrlTree(['/auth/login'], {
        queryParams: { returnUrl: state.url },
      });
    }),
  );
};
