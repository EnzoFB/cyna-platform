import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { LoggingService } from '../services/logging.service';

/**
 * Intercepts HTTP errors and forwards them to the centralized logging service.
 * Excludes the logging endpoint itself to avoid infinite loops.
 */
export const httpErrorLoggingInterceptor: HttpInterceptorFn = (req, next) => {
  const logging = inject(LoggingService);

  // Avoid logging loop: don't log failures on the logging endpoint
  if (req.url.includes('/api/v1/logs/client')) {
    return next(req);
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message = `HTTP ${error.status} ${error.statusText} on ${req.method} ${req.url}`;
      logging.logError(
        `${message}${error.error ? ': ' + JSON.stringify(error.error).substring(0, 500) : ''}`,
        { url: req.url }
      );
      return throwError(() => error);
    })
  );
};
