import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from './user.service';

export interface AdminOrder {
  id: string;
  userId: string;
  customerEmail: string;
  customerFirstName: string;
  customerLastName: string;
  status: 'PENDING' | 'CONFIRMED' | 'PAID' | 'FULFILLED' | 'CANCELLED';
  subtotalHt: number;
  currency: string;
  lineCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminOrderLine {
  id: string;
  productId: string;
  productName: string;
  productCategory: string;
  billingCycle: string;
  quantity: number;
  unitPrice: number;
  currency: string;
}

export interface AdminOrderDetail {
  id: string;
  userId: string;
  customerEmail: string;
  customerFirstName: string;
  customerLastName: string;
  status: string;
  subtotalHt: number;
  currency: string;
  createdAt: string;
  updatedAt: string;
  lines: AdminOrderLine[];
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  getOrders(page: number = 0, size: number = 20, status?: string) {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<ApiResponse<PagedResponse<AdminOrder>>>(
      `${environment.apiUrl}/admin/orders`,
      { params }
    );
  }

  getOrderById(id: string) {
    return this.http.get<ApiResponse<AdminOrderDetail>>(
      `${environment.apiUrl}/admin/orders/${id}`
    );
  }

  cancelOrder(id: string, reason: string) {
    return this.http.post<ApiResponse<void>>(
      `${environment.apiUrl}/admin/orders/${id}/cancel`,
      { reason }
    );
  }
}
