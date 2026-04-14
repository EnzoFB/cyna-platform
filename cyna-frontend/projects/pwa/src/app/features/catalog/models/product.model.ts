export type ProductSort = 'default' | 'price-asc' | 'price-desc';

export interface Product {
  readonly id: string;
  readonly name: string;
  readonly imageUrl: string | null;
  readonly category: string;
  readonly monthlyPrice: number;
  readonly currency: string;
  readonly status: string;
  readonly priority: string;
}

export interface ProductDetail extends Product {
  readonly serviceDescription: string;
  readonly technicalDescription: string;
  readonly availableImmediately: boolean;
  readonly annualBillingAvailable: boolean;
  readonly annualMonthlyPrice: number;
  readonly highlightPoints: readonly string[];
}
