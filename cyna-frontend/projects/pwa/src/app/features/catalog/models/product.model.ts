export type ProductSort = 'default' | 'price-asc' | 'price-desc';

export interface ProductTranslation {
  readonly name: string;
  readonly serviceDescription: string;
  readonly technicalDescription: string;
  readonly highlightPoints: readonly string[];
}

export interface Product {
  readonly id: string;
  readonly translations: Record<string, ProductTranslation>;
  readonly categoryId: string;
  readonly categoryName: string;
  readonly priorityLevel: number;
  readonly monthlyPrice: number;
  readonly annualPrice: number;
  readonly originalMonthlyPrice?: number | null;
  readonly originalAnnualPrice?: number | null;
  readonly promotionDiscountPercent?: number | null;
  readonly currency: string;
  readonly primaryImageBase64: string | null;
  readonly isPublished: boolean;
  readonly isAvailable: boolean;
}

export interface ProductDetail extends Product {
  readonly freeTrialDays: number;
  readonly images: readonly { id: string; base64: string }[];
}
