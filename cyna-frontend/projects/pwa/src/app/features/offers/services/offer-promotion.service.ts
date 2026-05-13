import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { catchError, map, Observable, of } from 'rxjs';

import { ApiResponse } from '../../../core/models/api-response.model';
import { environment } from '../../../../environments/environment';
import { OfferPromotion } from '../models/offer-promotion.model';

interface OfferPromotionDto {
  readonly id: string;
  readonly title: string;
  readonly description: string;
  readonly ctaLabel?: string | null;
  readonly ctaRoute?: string | null;
  readonly ctaUrl?: string | null;
  readonly background?: string | null;
  readonly imageUrl?: string | null;
  readonly active?: boolean;
  readonly order?: number;
}

@Injectable({ providedIn: 'root' })
export class OfferPromotionService {
  private readonly http = inject(HttpClient);
  private readonly translate = inject(TranslateService);

  private readonly endpoint = `${environment.apiUrl}/offers/promotions`;

  getPromotions(language: string): Observable<readonly OfferPromotion[]> {
    return this.http
      .get<ApiResponse<readonly OfferPromotionDto[]>>(this.endpoint, {
        params: { lang: language }
      })
      .pipe(
        map(response => response.data ?? []),
        map(promotions => this.mapPromotions(promotions)),
        map(promotions => (promotions.length > 0 ? promotions : this.fallbackPromotions())),
        catchError(() => of(this.fallbackPromotions()))
      );
  }

  private mapPromotions(promotions: readonly OfferPromotionDto[]): readonly OfferPromotion[] {
    return [...promotions]
      .filter(promotion => promotion.active !== false)
      .filter(promotion => Boolean(promotion.id?.trim() && promotion.title?.trim() && promotion.description?.trim()))
      .sort((first, second) => (first.order ?? Number.MAX_SAFE_INTEGER) - (second.order ?? Number.MAX_SAFE_INTEGER))
      .map(promotion => ({
        id: promotion.id,
        title: promotion.title.trim(),
        description: promotion.description.trim(),
        ctaLabel: promotion.ctaLabel?.trim() || undefined,
        ctaRoute: promotion.ctaRoute?.trim() || undefined,
        ctaUrl: promotion.ctaUrl?.trim() || undefined,
        imageUrl: promotion.imageUrl?.trim() || undefined,
        background: promotion.background?.trim() || this.defaultBackground()
      }));
  }

  private fallbackPromotions(): readonly OfferPromotion[] {
    return [
      {
        id: 'promo-onboarding',
        title: this.translate.instant('offers.promotions.fallback.onboarding.title'),
        description: this.translate.instant('offers.promotions.fallback.onboarding.description'),
        ctaLabel: this.translate.instant('offers.promotions.fallback.onboarding.cta'),
        ctaRoute: '/offers',
        background: 'linear-gradient(132deg, #0e2348 0%, #145a8f 52%, #11c5df 100%)'
      },
      {
        id: 'promo-trial',
        title: this.translate.instant('offers.promotions.fallback.trial.title'),
        description: this.translate.instant('offers.promotions.fallback.trial.description'),
        ctaLabel: this.translate.instant('offers.promotions.fallback.trial.cta'),
        ctaRoute: '/catalog',
        background: 'linear-gradient(132deg, #0f1f3d 0%, #1d3f6e 45%, #7ad8f8 100%)'
      },
      {
        id: 'promo-subscriptions',
        title: this.translate.instant('offers.promotions.fallback.subscriptions.title'),
        description: this.translate.instant('offers.promotions.fallback.subscriptions.description'),
        ctaLabel: this.translate.instant('offers.promotions.fallback.subscriptions.cta'),
        ctaRoute: '/account',
        background: 'linear-gradient(132deg, #16213a 0%, #294f84 55%, #4ad9c8 100%)'
      }
    ];
  }

  private defaultBackground(): string {
    return 'linear-gradient(132deg, #0e2348 0%, #145a8f 52%, #11c5df 100%)';
  }
}
