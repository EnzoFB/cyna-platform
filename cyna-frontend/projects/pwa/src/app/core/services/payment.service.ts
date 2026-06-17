import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ApiResponse } from '../models/api-response.model';
import { TranslateService } from '@ngx-translate/core';

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

/**
 * One PSD2/3DS challenge to complete with
 * {@code stripe.confirmCardPayment(paymentIntentClientSecret)}. After every
 * action confirms successfully, the order is considered paid (the backend
 * will reconcile via the {@code customer.subscription.updated} webhook).
 */
export interface PendingPaymentAction {
  stripeSubscriptionId: string;
  paymentIntentClientSecret: string;
}

export interface FinalizePaymentResponse {
  paymentId: string;
  orderId: string;
  /**
   * True when at least one off-session charge triggered a 3DS challenge. The
   * front MUST resolve every {@link pendingActions} with Stripe.js before
   * treating the payment as complete. {@link lines} is empty in this case.
   */
  requiresAction: boolean;
  lines: FinalizedLine[];
  pendingActions: PendingPaymentAction[];
}

/** One prospective cart line for a tax preview (prices resolved server-side). */
export interface TaxPreviewLine {
  productId: string;
  billingCycle: 'MONTHLY' | 'ANNUAL';
  quantity: number;
}

export interface TaxPreviewRequest {
  currency: string;
  lines: TaxPreviewLine[];
  countryCode: string;
  postalCode?: string | null;
  state?: string | null;
  vatNumber?: string | null;
}

/**
 * Exact VAT for a prospective checkout, computed by Stripe Tax.
 * - {@code exact=false} → Stripe Tax is off / preview unavailable; the caller
 *   keeps its own client-side estimate (amounts are null).
 * - {@code reverseCharge=true} → intra-EU B2B autoliquidation (VAT 0%).
 */
export interface TaxPreviewResponse {
  exact: boolean;
  subtotalHt: number | null;
  vatAmount: number | null;
  totalTtc: number | null;
  currency: string | null;
  reverseCharge: boolean;
}

/**
 * Authoritative VAT/TTC of a paid order, read by the backend from the order's
 * Stripe invoices.
 * - {@code available=false} → the invoice isn't ready yet (or Stripe was
 *   unreachable); amounts are null and the UI falls back to the HT subtotal.
 * - {@code reverseCharge=true} → intra-EU B2B autoliquidation (VAT 0%).
 */
export interface OrderTaxSummaryResponse {
  available: boolean;
  subtotalHt: number | null;
  vatAmount: number | null;
  totalTtc: number | null;
  currency: string | null;
  reverseCharge: boolean;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly translate = inject(TranslateService);

  initiatePayment(orderId: string): Observable<InitiatePaymentResponse> {
    return this.http
      .post<ApiResponse<InitiatePaymentResponse>>(
        `${environment.apiUrl}/payments/initiate`,
        { orderId },
        { headers: this.authHeaders() }
      )
      .pipe(map(r => r.data));
  }

  /**
   * @param vatNumber optional B2B VAT number. When supplied for a valid
   *   cross-border EU customer, the backend attaches it to the Stripe Customer
   *   so Stripe Tax applies the reverse charge (0% VAT). Omit/null for B2C.
   */
  finalizePayment(
    orderId: string,
    paymentMethodId: string,
    vatNumber?: string | null,
  ): Observable<FinalizePaymentResponse> {
    const lang = this.translate.currentLang || 'fr';
    return this.http
      .post<ApiResponse<FinalizePaymentResponse>>(
        `${environment.apiUrl}/payments/finalize`,
        { orderId, paymentMethodId, vatNumber: vatNumber || null, lang },
        { headers: this.authHeaders() }
      )
      .pipe(map(r => r.data));
  }

  /**
   * Asks the backend for the exact VAT (incl. B2B reverse charge) of a
   * prospective checkout, so the order summary can show the authoritative amount
   * before the customer pays. Used reactively as the address / VAT number change.
   */
  previewTax(request: TaxPreviewRequest): Observable<TaxPreviewResponse> {
    return this.http
      .post<ApiResponse<TaxPreviewResponse>>(
        `${environment.apiUrl}/payments/tax-preview`,
        request,
        { headers: this.authHeaders() }
      )
      .pipe(map(r => r.data));
  }

  /**
   * Fetches the authoritative VAT/TTC of a paid order from its Stripe invoices,
   * so the confirmation page can display the billed total (incl. VAT). Returns
   * {@code available=false} while the invoice is still being finalised.
   */
  getOrderTaxSummary(orderId: string): Observable<OrderTaxSummaryResponse> {
    return this.http
      .get<ApiResponse<OrderTaxSummaryResponse>>(
        `${environment.apiUrl}/payments/order/${orderId}/tax-summary`,
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
