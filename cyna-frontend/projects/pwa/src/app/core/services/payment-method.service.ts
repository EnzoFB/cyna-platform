import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { SavedPaymentMethod } from '../models/saved-payment-method.model';

@Injectable({ providedIn: 'root' })
export class PaymentMethodService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/account/payment-methods`;

  getAll(): Observable<SavedPaymentMethod[]> {
    return this.http
      .get<ApiResponse<SavedPaymentMethod[]>>(this.base)
      .pipe(map(r => r.data ?? []));
  }

  /**
   * Persist a PaymentMethod attached during the checkout SetupIntent flow.
   * The only remaining caller is `CheckoutComponent` when the user ticks
   * the "save this card" consent checkbox.
   */
  save(stripePaymentMethodId: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(this.base, { stripePaymentMethodId })
      .pipe(map(() => void 0));
  }
}
