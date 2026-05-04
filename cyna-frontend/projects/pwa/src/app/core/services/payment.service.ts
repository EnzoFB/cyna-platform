import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ApiResponse } from '../models/api-response.model';

export interface PaymentIntentResponse {
  paymentId: string;
  orderId: string;
  clientSecret: string;
  amount: number;
  currency: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  initiatePayment(orderId: string): Observable<PaymentIntentResponse> {
    return this.http
      .post<ApiResponse<PaymentIntentResponse>>(
        `${environment.apiUrl}/payments/initiate`,
        { orderId },
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
