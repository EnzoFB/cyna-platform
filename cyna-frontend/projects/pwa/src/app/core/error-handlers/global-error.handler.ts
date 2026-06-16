import { ErrorHandler, inject } from '@angular/core';
import { LoggingService } from '../services/logging.service';
import { Router } from '@angular/router';

export class GlobalErrorHandler implements ErrorHandler {
  private logging = inject(LoggingService);
  private router = inject(Router);

  handleError(error: unknown): void {
    console.error(error);
    try {
      this.logging.logError(error, { url: this.router.url });
    } catch (e) {
      console.error('GlobalErrorHandler failed to log:', e);
    }
  }
}
