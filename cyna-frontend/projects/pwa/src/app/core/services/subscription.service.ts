import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ApiResponse, PagedResponse } from '../models/api-response.model';

export interface SubscriptionResponse {
  id: string;
  userId: string;
  orderId: string;
  productId: string;
  productName: string;
  productCategory: string;
  billingCycle: 'MONTHLY' | 'ANNUAL';
  status: 'ACTIVE' | 'PAST_DUE' | 'CANCELLED' | 'EXPIRED' | 'PAUSED' | 'PENDING';
  quantity: number;
  unitPrice: number;
  currency: string;
  startAt: string;
  endAt: string;
  nextBillingAt: string;
  cancelledAt: string | null;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  list(page = 0, size = 20): Observable<PagedResponse<SubscriptionResponse>> {
    return this.http
      .get<ApiResponse<PagedResponse<SubscriptionResponse>>>(
        `${environment.apiUrl}/subscriptions?page=${page}&size=${size}&sort=createdAt,desc`,
        { headers: this.authHeaders() }
      )
      .pipe(map(r => r.data));
  }

  cancel(subscriptionId: string): Observable<SubscriptionResponse> {
    return this.http
      .post<ApiResponse<SubscriptionResponse>>(
        `${environment.apiUrl}/subscriptions/${subscriptionId}/cancel`,
        {},
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
