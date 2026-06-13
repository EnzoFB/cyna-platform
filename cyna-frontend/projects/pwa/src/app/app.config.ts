import {
  ApplicationConfig,
  inject,
  isDevMode,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideServiceWorker } from '@angular/service-worker';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/services/auth.service';

import { provideTranslateService, TranslateLoader } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { MultiFileTranslateLoader } from './core/loaders/multi-file-translate.loader';
import {provideAnimations} from "@angular/platform-browser/animations";
import {provideToastr} from "ngx-toastr";

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    // Block app bootstrap on the initial /auth/refresh round-trip. By the
    // time Angular mounts the first component (and feature services fire
    // their HTTP calls) the AuthService either holds a valid access token
    // or has settled into "not logged in". Eliminates the bootstrap race
    // that previously let on-401 refreshes collide with restoreSession
    // and trip the backend's reuse-detection.
    provideAppInitializer(() => firstValueFrom(inject(AuthService).restoreSession())),
    provideAnimations(),
    provideToastr(),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000'
    }),

    provideTranslateService({
      defaultLanguage: 'fr',
      loader: {
        provide: TranslateLoader,
        useFactory: (http: HttpClient) => new MultiFileTranslateLoader(http),
        deps: [HttpClient],
      },
    })
  ],
};
