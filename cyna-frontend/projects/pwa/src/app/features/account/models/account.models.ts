export type AccountBillingCycle = 'MONTHLY' | 'ANNUAL';

export type AccountSubscriptionStatus =
  | 'PENDING'
  | 'ACTIVE'
  | 'PAUSED'
  | 'CANCELLED'
  | 'EXPIRED';

export interface AccountSubscription {
  readonly id: string;
  readonly userId: string;
  readonly orderId: string;
  readonly productId: string;
  readonly productName: string;
  readonly productCategory: string;
  readonly billingCycle: AccountBillingCycle;
  readonly status: AccountSubscriptionStatus;
  readonly quantity: number;
  readonly unitPrice: number;
  readonly currency: string;
  readonly startAt: string;
  readonly endAt: string;
  readonly nextBillingAt: string | null;
  readonly cancelledAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}
