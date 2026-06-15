import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import { ApiResponse } from '../models/api-response.model';
import { CartBillingCycle } from './cart.service';

export interface CreateOrderBillingAddress {
  line1: string;
  city: string;
  zipCode: string;
  countryCode: string;
}

export interface CreateOrderLine {
  productId: string;
  billingCycle: CartBillingCycle;
  quantity: number;
}

export interface OrderLineResponse {
  id: string;
  productId: string;
  productName: string;
  productCategory: string;
  billingCycle: string;
  quantity: number;
  unitPrice: number;
  currency: string;
}

export interface OrderResponse {
  id: string;
  userId: string;
  status: string;
  subtotalAmount: number;
  vatAmount: number;
  totalAmount: number;
  currency: string;
  createdAt: string;
  lines: OrderLineResponse[];
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);

  createOrder(lines: CreateOrderLine[], billingAddress?: CreateOrderBillingAddress | null): Observable<string> {
    return this.http
      .post<ApiResponse<string>>(
        `${environment.apiUrl}/orders`,
        { lines, billingAddress: billingAddress ?? null },
        { headers: this.authHeaders() }
      )
      .pipe(map(r => r.data));
  }

  getOrder(id: string): Observable<OrderResponse> {
    return this.http
      .get<ApiResponse<OrderResponse>>(
        `${environment.apiUrl}/orders/${id}`,
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
