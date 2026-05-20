export type ProductSort = 'default' | 'price-asc' | 'price-desc';

export interface Product {
  readonly id: string;
  readonly name: string;
  readonly nameEn: string;
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
  readonly serviceDescription: string;
  readonly serviceDescriptionEn: string;
  readonly technicalDescription: string;
  readonly technicalDescriptionEn: string;
  readonly freeTrialDays: number;
  readonly highlightPoints: readonly string[];
  readonly highlightPointsEn: readonly string[];
  readonly images: readonly { id: string; base64: string }[];
}
