import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PagedResponse } from '../../../core/models/api-response.model';
import { AccountSubscription } from '../models/account.models';

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
  readonly createdAt: string;
  readonly updatedAt: string;
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
      createdAt: item.createdAt,
      updatedAt: item.updatedAt
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
