import { HttpClient } from '@angular/common/http';
import { TranslateLoader, TranslationObject } from '@ngx-translate/core';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

const I18N_FILES = [
  'global',
  'home',
  'header',
  'footer',
  'auth',
  'offers',
  'catalog',
  'product-detail',
  'cart',
  'checkout',
  'my-subscriptions',
  'order-confirmation',
  'account',
  'privacy-policy',
  'terms',
  'legal-notice',
  'contact',
  'about',
  'error',
];

export class MultiFileTranslateLoader implements TranslateLoader {
  constructor(private http: HttpClient) {}

  getTranslation(lang: string): Observable<TranslationObject> {
    const requests = I18N_FILES.map(file =>
      this.http.get<TranslationObject>(`/i18n/${lang}/${file}.json`)
    );
    return forkJoin(requests).pipe(
      map(results => Object.assign({}, ...results))
    );
  }
}
