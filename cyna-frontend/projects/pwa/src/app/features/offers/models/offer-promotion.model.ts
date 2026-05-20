export interface OfferPromotion {
  readonly id: string;
  readonly productId: string;
  readonly productName: string;
  readonly productCategoryName: string;
  readonly marketingText: string;
  readonly discountPercent: number;
  readonly originalMonthlyPrice: number;
  readonly promotionalMonthlyPrice: number;
  readonly originalAnnualPrice: number;
  readonly promotionalAnnualPrice: number;
  readonly currency: string;
  readonly primaryImageBase64?: string | null;
  readonly carouselOrder: number;
}
