import {
  ApplicationConfig,
  ErrorHandler,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { GlobalErrorHandler } from './core/error-handlers/global-error.handler';
import { correlationIdInterceptor } from './core/interceptors/correlation-id.interceptor';
import { firstValueFrom } from 'rxjs';
import { provideTranslateService, TranslateLoader } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { MultiFileTranslateLoader } from './core/loaders/multi-file-translate.loader';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { httpErrorLoggingInterceptor } from './core/interceptors/http-error-logging.interceptor';
import { AuthService } from './core/services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor, correlationIdInterceptor, httpErrorLoggingInterceptor])),
    provideTranslateService({
      defaultLanguage: 'fr',
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => new MultiFileTranslateLoader(http),
        deps: [HttpClient],
      },
    }),
    // Block app bootstrap on the initial /auth/refresh round-trip so the
    // auth state is settled before any feature route mounts. Mirrors the
    // PWA wiring. Eliminates the bootstrap race that previously let
    // on-401 refreshes collide with restoreSession.
    provideAppInitializer(() => firstValueFrom(inject(AuthService).restoreSession())),
  ],
};
