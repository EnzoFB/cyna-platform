import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { provideTranslateService, TranslateLoader } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { MultiFileTranslateLoader } from './core/loaders/multi-file-translate.loader';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
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
