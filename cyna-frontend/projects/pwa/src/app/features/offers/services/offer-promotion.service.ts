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
}

@Injectable({ providedIn: 'root' })
export class OfferPromotionService {
  private readonly http = inject(HttpClient);

  private readonly endpoint = `${environment.apiUrl}/offers/promotions`;

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
        primaryImageBase64: promotion.primaryImageBase64 ?? null
      }));
  }
}
