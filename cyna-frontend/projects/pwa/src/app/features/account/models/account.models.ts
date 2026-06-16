export type AccountBillingCycle = 'MONTHLY' | 'ANNUAL';

export type AccountSubscriptionStatus =
  | 'PENDING'
  | 'ACTIVE'
  | 'PAUSED'
  | 'PAST_DUE'
  | 'CANCELLED'
  | 'EXPIRED';

export type AccountOrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PAID'
  | 'FULFILLED'
  | 'CANCELLED';

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
  readonly autoRenew: boolean;
  readonly autoRenewNoticeSentAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface AccountOrderLine {
  readonly id: string;
  readonly productId: string;
  readonly productName: string;
  readonly productCategory: string;
  readonly billingCycle: AccountBillingCycle;
  readonly quantity: number;
  readonly unitPrice: number;
  readonly currency: string;
}

export interface AccountBillingAddress {
  readonly line1: string;
  readonly city: string;
  readonly zipCode: string;
  readonly countryCode: string;
}

export interface AccountOrder {
  readonly id: string;
  readonly userId: string;
  readonly status: AccountOrderStatus;
  readonly subtotalAmount: number;
  readonly vatAmount: number;
  readonly totalAmount: number;
  readonly currency: string;
  readonly billingAddress: AccountBillingAddress | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly lines: readonly AccountOrderLine[];
}

export interface AccountInvoice {
  readonly id: string;
  readonly number: string | null;
  readonly status: string;
  readonly amountPaid: number;
  readonly currency: string;
  readonly createdAt: string;
  readonly hostedInvoiceUrl: string | null;
  readonly invoicePdfUrl: string | null;
}

export interface AccountAddress {
  readonly id: string;
  readonly label: string;
  readonly address: string;
  readonly address2: string;
  readonly city: string;
  readonly zipCode: string;
  readonly region: string;
  readonly country: string;
  readonly phone: string;
  readonly isDefault: boolean;
}

export interface AccountPaymentMethod {
  readonly id: string;
  readonly holder: string;
  readonly brand: string;
  readonly last4: string;
  readonly expiryMonth: string;
  readonly expiryYear: string;
  readonly isDefault: boolean;
}
