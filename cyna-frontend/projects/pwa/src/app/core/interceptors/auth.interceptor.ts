import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  if (req.url.includes('/auth/refresh') || req.url.includes('/auth/login') || req.url.includes('/auth/register')) {
    return next(req);
  }

  const token = authService.accessToken;
  const authedReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;
  const hasAuthHeader = authedReq.headers.has('Authorization');

  return next(authedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !authService.isRefreshing && hasAuthHeader) {
        return authService.refreshToken().pipe(
          switchMap(() => {
            const newToken = authService.accessToken;
            if (!newToken) {
              authService.logout();
              return throwError(() => error);
            }
            const retryReq = req.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` },
            });
            return next(retryReq);
          }),
          catchError(refreshError => {
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
