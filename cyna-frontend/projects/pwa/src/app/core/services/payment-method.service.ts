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

  createSetupIntent(): Observable<string> {
    return this.http
      .post<ApiResponse<{ clientSecret: string }>>(`${this.base}/setup-intent`, {})
      .pipe(map(r => r.data.clientSecret));
  }

  save(stripePaymentMethodId: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(this.base, { stripePaymentMethodId })
      .pipe(map(() => void 0));
  }

  delete(id: string): Observable<void> {
    return this.http
      .delete<void>(`${this.base}/${id}`)
      .pipe(map(() => void 0));
  }

  setDefault(id: string): Observable<void> {
    return this.http
      .patch<ApiResponse<void>>(`${this.base}/${id}/default`, {})
      .pipe(map(() => void 0));
  }
}
