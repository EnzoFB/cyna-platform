import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PagedResponse } from '../../../core/models/api-response.model';
import { AccountOrder, AccountOrderLine, AccountSubscription } from '../models/account.models';

type RawAmount = number | string | null | undefined;

interface AccountSubscriptionDto {
  readonly id: string;
  readonly userId: string;
  readonly orderId: string;
  readonly productId: string;
  readonly productName: string;
  readonly productCategory: string;
  readonly billingCycle: AccountSubscription['billingCycle'];
  readonly status: AccountSubscription['status'];
  readonly quantity: number;
  readonly unitPrice: RawAmount;
  readonly currency: string;
  readonly startAt: string;
  readonly endAt: string;
  readonly nextBillingAt: string | null;
  readonly cancelledAt: string | null;
  readonly autoRenew: boolean;
  readonly autoRenewNoticeSentAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface AccountOrderLineDto {
  readonly id: string;
  readonly productId: string;
  readonly productName: string;
  readonly productCategory: string;
  readonly billingCycle: AccountOrderLine['billingCycle'];
  readonly quantity: number;
  readonly unitPrice: RawAmount;
  readonly currency: string;
}

interface AccountOrderDto {
  readonly id: string;
  readonly userId: string;
  readonly status: AccountOrder['status'];
  readonly subtotalAmount: RawAmount;
  readonly vatAmount: RawAmount;
  readonly totalAmount: RawAmount;
  readonly currency: string;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly lines: readonly AccountOrderLineDto[];
}

@Injectable({ providedIn: 'root' })
export class AccountDashboardService {
  private readonly http = inject(HttpClient);

  listSubscriptions(page = 0, size = 50): Observable<readonly AccountSubscription[]> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sort', 'createdAt,desc');

    return this.http
      .get<ApiResponse<PagedResponse<AccountSubscriptionDto>>>(`${environment.apiUrl}/subscriptions`, { params })
      .pipe(
        map((res) => (res.data?.items ?? []).map((item) => this.normalizeSubscription(item)))
      );
  }

  listOrders(page = 0, size = 50): Observable<readonly AccountOrder[]> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size))
      .set('sort', 'createdAt,desc');

    return this.http
      .get<ApiResponse<PagedResponse<AccountOrderDto>>>(`${environment.apiUrl}/orders`, { params })
      .pipe(
        map(res => (res.data?.items ?? []).map(item => this.normalizeOrder(item)))
      );
  }

  updateSubscriptionAutoRenew(subscriptionId: string, autoRenew: boolean): Observable<AccountSubscription> {
    return this.http
      .put<ApiResponse<AccountSubscriptionDto>>(
        `${environment.apiUrl}/subscriptions/${subscriptionId}/auto-renew`,
        { autoRenew }
      )
      .pipe(
        map(res => this.normalizeSubscription(res.data))
      );
  }

  private normalizeSubscription(item: AccountSubscriptionDto): AccountSubscription {
    return {
      id: item.id,
      userId: item.userId,
      orderId: item.orderId,
      productId: item.productId,
      productName: item.productName,
      productCategory: item.productCategory,
      billingCycle: item.billingCycle,
      status: item.status,
      quantity: item.quantity,
      unitPrice: this.toNumber(item.unitPrice),
      currency: item.currency,
      startAt: item.startAt,
      endAt: item.endAt,
      nextBillingAt: item.nextBillingAt,
      cancelledAt: item.cancelledAt,
      autoRenew: item.autoRenew,
      autoRenewNoticeSentAt: item.autoRenewNoticeSentAt,
      createdAt: item.createdAt,
      updatedAt: item.updatedAt
    };
  }

  private normalizeOrder(item: AccountOrderDto): AccountOrder {
    return {
      id: item.id,
      userId: item.userId,
      status: item.status,
      subtotalAmount: this.toNumber(item.subtotalAmount),
      vatAmount: this.toNumber(item.vatAmount),
      totalAmount: this.toNumber(item.totalAmount),
      currency: item.currency,
      createdAt: item.createdAt,
      updatedAt: item.updatedAt,
      lines: (item.lines ?? []).map(line => ({
        id: line.id,
        productId: line.productId,
        productName: line.productName,
        productCategory: line.productCategory,
        billingCycle: line.billingCycle,
        quantity: line.quantity,
        unitPrice: this.toNumber(line.unitPrice),
        currency: line.currency
      }))
    };
  }

  private toNumber(value: RawAmount): number {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }
    if (typeof value === 'string' && value.trim().length > 0) {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : 0;
    }
    return 0;
  }
}
