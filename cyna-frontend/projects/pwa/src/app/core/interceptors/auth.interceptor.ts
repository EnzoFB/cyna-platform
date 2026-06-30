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
      // Only trigger refresh on 401 from a request we actually authenticated.
      // A 401 on an anonymous request means "this endpoint requires auth",
      // not "your access token expired" — refreshing wouldn't help.
      if (error.status !== 401 || !hasAuthHeader) {
        return throwError(() => error);
      }
      // refreshToken() is internally deduped via a single shared in-flight
      // observable, so multiple parallel 401s all subscribe to the same
      // POST /auth/refresh — no risk of presenting the same refresh token
      // twice to the backend (which would trip reuse detection).
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
        }),
      );
    }),
  );
};
