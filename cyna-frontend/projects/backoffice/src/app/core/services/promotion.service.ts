import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface PromotionTranslation {
  marketingText: string;
}

export interface CarouselSettingsTranslation {
  fixedText: string;
}

export interface AdminPromotion {
  id: string;
  productId: string;
  productName: string;
  productCategoryName: string;
  baseMonthlyPrice: number;
  baseAnnualPrice: number;
  discountedMonthlyPrice: number;
  discountedAnnualPrice: number;
  currency: string;
  discountPercent: number;
  translations: Record<string, PromotionTranslation>;
  startAt: string;
  endAt: string;
  enabled: boolean;
  showInCarousel: boolean;
  carouselOrder: number | null;
  activeNow: boolean;
  productAvailable: boolean;
  productPublished: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePromotionPayload {
  productId: string;
  discountPercent: number;
  translations: Record<string, PromotionTranslation>;
  startAt: string;
  endAt: string;
  enabled: boolean;
}

export interface UpdatePromotionPayload {
  discountPercent: number;
  translations: Record<string, PromotionTranslation>;
  startAt: string;
  endAt: string;
  enabled: boolean;
}

export interface OfferCarouselSettings {
  translations: Record<string, CarouselSettingsTranslation>;
  maxSlides: number;
}

export interface UpdateOfferCarouselSettingsPayload {
  translations: Record<string, CarouselSettingsTranslation>;
  maxSlides: number;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
  error?: {
    code?: string;
    message?: string;
  };
}

@Injectable({ providedIn: 'root' })
export class PromotionService {
  private readonly http = inject(HttpClient);

  getPromotions() {
    return this.http.get<ApiResponse<AdminPromotion[]>>(
      `${environment.apiUrl}/admin/promotions`,
      { headers: { 'Cache-Control': 'no-cache' } }
    );
  }

  getPromotion(id: string) {
    return this.http.get<ApiResponse<AdminPromotion>>(
      `${environment.apiUrl}/admin/promotions/${id}`,
      { headers: { 'Cache-Control': 'no-cache' } }
    );
  }

  createPromotion(payload: CreatePromotionPayload) {
    return this.http.post<ApiResponse<string>>(
      `${environment.apiUrl}/admin/promotions`,
      payload
    );
  }

  updatePromotion(id: string, payload: UpdatePromotionPayload) {
    return this.http.put<ApiResponse<string>>(
      `${environment.apiUrl}/admin/promotions/${id}`,
      payload
    );
  }

  deletePromotion(id: string) {
    return this.http.delete<void>(`${environment.apiUrl}/admin/promotions/${id}`);
  }

  addToCarousel(id: string) {
    return this.http.post<void>(`${environment.apiUrl}/admin/promotions/${id}/carousel`, null);
  }

  removeFromCarousel(id: string) {
    return this.http.delete<void>(`${environment.apiUrl}/admin/promotions/${id}/carousel`);
  }

  getCarouselSettings() {
    return this.http.get<ApiResponse<OfferCarouselSettings>>(
      `${environment.apiUrl}/admin/promotions/carousel-settings`,
      { headers: { 'Cache-Control': 'no-cache' } }
    );
  }

  updateCarouselSettings(payload: UpdateOfferCarouselSettingsPayload) {
    return this.http.put<void>(
      `${environment.apiUrl}/admin/promotions/carousel-settings`,
      payload
    );
  }

  reorderCarousel(orderedIds: string[]) {
    return this.http.put<void>(
      `${environment.apiUrl}/admin/promotions/carousel-reorder`,
      { orderedIds }
    );
  }
}
