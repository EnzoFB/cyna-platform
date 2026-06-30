export interface SavedPaymentMethod {
  readonly id: string;
  readonly stripePaymentMethodId: string;
  readonly brand: string;
  readonly last4: string;
  readonly expMonth: string;
  readonly expYear: string;
  readonly holderName: string | null;
  readonly isDefault: boolean;
}
