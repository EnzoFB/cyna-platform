import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, of } from 'rxjs';

import { ApiResponse } from '../../../core/models/api-response.model';
import { environment } from '../../../../environments/environment';
import { OfferPromotion } from '../models/offer-promotion.model';

interface OfferPromotionDto {
  readonly promotionId: string;
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
  readonly carouselOrder?: number;
}

@Injectable({ providedIn: 'root' })
export class OfferPromotionService {
  private readonly http = inject(HttpClient);

  private readonly endpoint = `${environment.apiUrl}/offers/promotions`;
  private readonly fixedTextEndpoint = `${environment.apiUrl}/offers/promotions/fixed-text`;

  getPromotions(language: string): Observable<readonly OfferPromotion[]> {
    return this.http
      .get<ApiResponse<readonly OfferPromotionDto[]>>(this.endpoint, {
        params: { lang: language },
        headers: { 'Cache-Control': 'no-cache' }
      })
      .pipe(
        map(response => response.data ?? []),
        map(promotions => this.mapPromotions(promotions)),
        catchError(() => of([]))
      );
  }

  private mapPromotions(promotions: readonly OfferPromotionDto[]): readonly OfferPromotion[] {
    return [...promotions]
      .filter(promotion => Boolean(
        promotion.promotionId?.trim()
        && promotion.productId?.trim()
        && promotion.productName?.trim()
        && promotion.marketingText?.trim()
      ))
      .sort((first, second) => {
        const firstOrder = typeof first.carouselOrder === 'number' ? first.carouselOrder : Number.MAX_SAFE_INTEGER;
        const secondOrder = typeof second.carouselOrder === 'number' ? second.carouselOrder : Number.MAX_SAFE_INTEGER;
        if (firstOrder !== secondOrder) {
          return firstOrder - secondOrder;
        }
        return first.productName.localeCompare(second.productName, 'fr');
      })
      .map(promotion => ({
        id: promotion.promotionId,
        productId: promotion.productId,
        productName: promotion.productName.trim(),
        productCategoryName: promotion.productCategoryName?.trim() || '',
        marketingText: promotion.marketingText.trim(),
        discountPercent: promotion.discountPercent,
        originalMonthlyPrice: promotion.originalMonthlyPrice,
        promotionalMonthlyPrice: promotion.promotionalMonthlyPrice,
        originalAnnualPrice: promotion.originalAnnualPrice,
        promotionalAnnualPrice: promotion.promotionalAnnualPrice,
        currency: promotion.currency,
        primaryImageBase64: promotion.primaryImageBase64 ?? null,
        carouselOrder: typeof promotion.carouselOrder === 'number' ? promotion.carouselOrder : Number.MAX_SAFE_INTEGER
      }));
  }

  getFixedText(language: string): Observable<string> {
    return this.http
      .get<ApiResponse<string>>(this.fixedTextEndpoint, {
        params: { lang: language },
        headers: { 'Cache-Control': 'no-cache' }
      })
      .pipe(
        map(response => (response.data ?? '').trim()),
        catchError(() => of(''))
      );
  }
}
