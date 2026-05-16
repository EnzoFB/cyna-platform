import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';

/**
 * Version identifier of the consent label currently displayed at checkout
 * (i18n keys `checkout.payment.saveConsent` + `saveConsentHelp`). Bump the
 * value whenever the wording changes — that way GDPR proofs stay tied to
 * the exact text the user accepted.
 */
export const CARD_CONSENT_LABEL_VERSION = '2026-05-14';

@Injectable({ providedIn: 'root' })
export class ConsentLogService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/account/consent-log`;

  /**
   * Records the user's explicit consent for keeping the just-saved
   * PaymentMethod tied to their account. IP and User-Agent are read
   * server-side. Best-effort by design at the caller — failure here must
   * not block the user from completing their purchase.
   */
  logPaymentMethodConsent(stripePaymentMethodId: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.base}/payment-method`, {
        stripePaymentMethodId,
        labelVersion: CARD_CONSENT_LABEL_VERSION,
      })
      .pipe(map(() => void 0));
  }
}
