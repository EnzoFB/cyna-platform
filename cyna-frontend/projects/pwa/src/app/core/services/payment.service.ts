import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ApiResponse } from '../models/api-response.model';

/**
 * Backend `POST /payments/initiate` response. The {@code setupIntentClientSecret}
 * is what Stripe.js's `confirmCardSetup` (or `confirmSetup`) consumes to collect
 * a card and produce a PaymentMethod id, which is then handed back to
 * {@link PaymentService.finalizePayment}.
 */
export interface InitiatePaymentResponse {
  paymentId: string;
  orderId: string;
  setupIntentClientSecret: string;
  amount: number;
  currency: string;
}

/**
 * Per-OrderLine result returned by {@code POST /payments/finalize}. One Stripe
 * Subscription is created per line; {@code stripeStatus} reflects Stripe's
 * immediate post-creation status:
 *  - {@code "active"} / {@code "trialing"} — first invoice charged successfully
 *  - {@code "incomplete"} — first invoice could not be charged (3DS abandoned,
 *    declined card…); the customer needs to address it. Backend sync via the
 *    {@code customer.subscription.updated} webhook will keep DB and UI aligned.
 */
export interface FinalizedLine {
  orderLineId: string;
  subscriptionId: string;
  stripeSubscriptionId: string;
  stripeStatus: string;
}

export interface FinalizePaymentResponse {
  paymentId: string;
  orderId: string;
  lines: FinalizedLine[];
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  initiatePayment(orderId: string): Observable<InitiatePaymentResponse> {
    return this.http
      .post<ApiResponse<InitiatePaymentResponse>>(
        `${environment.apiUrl}/payments/initiate`,
        { orderId },
        { headers: this.authHeaders() }
      )
      .pipe(map(r => r.data));
  }

  finalizePayment(orderId: string, paymentMethodId: string): Observable<FinalizePaymentResponse> {
    return this.http
      .post<ApiResponse<FinalizePaymentResponse>>(
        `${environment.apiUrl}/payments/finalize`,
        { orderId, paymentMethodId },
        { headers: this.authHeaders() }
      )
      .pipe(map(r => r.data));
  }

  openBillingPortal(returnUrl: string): Observable<{ url: string }> {
    return this.http
      .post<ApiResponse<{ url: string }>>(
        `${environment.apiUrl}/payments/billing-portal`,
        { returnUrl },
        { headers: this.authHeaders() }
      )
      .pipe(map(r => r.data));
  }

  private authHeaders(): HttpHeaders {
    return new HttpHeaders({
      Authorization: `Bearer ${this.authService.accessToken ?? ''}`,
    });
  }
}
