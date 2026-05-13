export interface OfferPromotion {
  readonly id: string;
  readonly title: string;
  readonly description: string;
  readonly background: string;
  readonly imageUrl?: string;
  readonly ctaLabel?: string;
  readonly ctaRoute?: string;
  readonly ctaUrl?: string;
}
