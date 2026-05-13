export type ProductSort = 'default' | 'price-asc' | 'price-desc';

export interface Product {
  readonly id: string;
  readonly name: string;
  readonly categoryId: string;
  readonly categoryName: string;
  readonly priorityLevel: number;
  readonly monthlyPrice: number;
  readonly annualPrice: number;
  readonly currency: string;
  readonly primaryImageBase64: string | null;
  readonly isPublished: boolean;
  readonly isAvailable: boolean;
}

export interface ProductDetail extends Product {
  readonly serviceDescription: string;
  readonly technicalDescription: string;
  readonly freeTrialDays: number;
  readonly highlightPoints: readonly string[];
  readonly images: readonly { id: string; base64: string }[];
}
