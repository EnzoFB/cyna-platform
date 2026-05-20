import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { Product, ProductDetail, ProductTranslation } from '../models/product.model';
import { Category, CategoryTranslation } from '../models/category.model';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PagedResponse } from '../../../core/models/api-response.model';

interface ProductApiDto {
  readonly id: string;
  readonly translations: Record<string, {
    name: string;
    serviceDescription: string;
    technicalDescription: string;
    highlightPoints: string[];
  }>;
  readonly categoryId: string;
  readonly categoryName: string;
  readonly priorityLevel: number;
  readonly monthlyPrice: number;
  readonly annualPrice: number;
  readonly discountedMonthlyPrice?: number | null;
  readonly discountedAnnualPrice?: number | null;
  readonly promotionDiscountPercent?: number | null;
  readonly currency: string;
  readonly primaryImageBase64: string | null;
  readonly isPublished: boolean;
  readonly isAvailable: boolean;
}

interface ProductDetailApiDto extends ProductApiDto {
  readonly freeTrialDays: number;
  readonly images: readonly { id: string; base64: string }[];
}

interface CategoryApiDto {
  readonly id: string;
  readonly name: string;
  readonly translations: Record<string, {
    fullName: string;
    description: string;
  }>;
  readonly imageBase64: string | null;
  readonly active: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  getProductPage(params: {
    readonly page: number;
    readonly size: number;
    readonly categoryId?: string;
    readonly search?: string;
    readonly sort?: string | readonly string[];
    readonly published?: boolean;
    readonly available?: boolean;
  }): Observable<PagedResponse<Product>> {
    let queryParams = new HttpParams()
      .set('page', String(params.page))
      .set('size', String(params.size))
      .set('published', String(params.published ?? true));

    if (params.available !== undefined) {
      queryParams = queryParams.set('available', String(params.available));
    }
    if (params.categoryId) {
      queryParams = queryParams.set('categoryId', params.categoryId);
    }
    if (params.search) {
      queryParams = queryParams.set('search', params.search);
    }
    if (params.sort) {
      const sorts = Array.isArray(params.sort) ? params.sort : [params.sort];
      for (const s of sorts) {
        queryParams = queryParams.append('sort', s as string);
      }
    }

    return this.http
      .get<ApiResponse<PagedResponse<ProductApiDto>>>(`${environment.apiUrl}/products`, {
        params: queryParams,
        headers: { 'Cache-Control': 'no-cache' }
      })
      .pipe(map(response => this.mapProductPage(response.data)));
  }

  getCategories(): Observable<Category[]> {
    return this.http
      .get<ApiResponse<CategoryApiDto[]>>(`${environment.apiUrl}/categories`)
      .pipe(map(response => response.data.map(c => this.mapCategory(c))));
  }

  getProductById(productId: string): Observable<ProductDetail | null> {
    return this.http
      .get<ApiResponse<ProductDetailApiDto>>(`${environment.apiUrl}/products/${productId}`, {
        headers: { 'Cache-Control': 'no-cache' }
      })
      .pipe(map(response => this.mapProductDetail(response.data)));
  }

  private mapProductPage(page: PagedResponse<ProductApiDto>): PagedResponse<Product> {
    return {
      ...page,
      items: page.items.map(item => this.mapProduct(item))
    };
  }

  private mapProductDetail(dto: ProductDetailApiDto | null | undefined): ProductDetail | null {
    if (!dto) return null;
    return {
      ...this.mapProduct(dto),
      freeTrialDays: dto.freeTrialDays,
      images:        dto.images
    };
  }

  private mapProduct(dto: ProductApiDto): Product {
    const hasMonthlyPromotion = this.isDiscounted(dto.monthlyPrice, dto.discountedMonthlyPrice);
    const hasAnnualPromotion  = this.isDiscounted(dto.annualPrice, dto.discountedAnnualPrice);

    const translations: Record<string, ProductTranslation> = {};
    for (const [locale, t] of Object.entries(dto.translations ?? {})) {
      translations[locale] = {
        name:                 t.name,
        serviceDescription:   t.serviceDescription,
        technicalDescription:  t.technicalDescription,
        highlightPoints:      t.highlightPoints ?? []
      };
    }

    return {
      id:                      dto.id,
      translations,
      categoryId:              dto.categoryId,
      categoryName:            dto.categoryName,
      priorityLevel:           dto.priorityLevel,
      monthlyPrice:            hasMonthlyPromotion ? (dto.discountedMonthlyPrice as number) : dto.monthlyPrice,
      annualPrice:             hasAnnualPromotion  ? (dto.discountedAnnualPrice  as number) : dto.annualPrice,
      originalMonthlyPrice:    hasMonthlyPromotion ? dto.monthlyPrice : null,
      originalAnnualPrice:     hasAnnualPromotion  ? dto.annualPrice  : null,
      promotionDiscountPercent: dto.promotionDiscountPercent ?? null,
      currency:                dto.currency,
      primaryImageBase64:      dto.primaryImageBase64,
      isPublished:             dto.isPublished,
      isAvailable:             dto.isAvailable
    };
  }

  private mapCategory(c: CategoryApiDto): Category {
    const translations: Record<string, CategoryTranslation> = {};
    for (const [locale, t] of Object.entries(c.translations ?? {})) {
      translations[locale] = { fullName: t.fullName, description: t.description };
    }
    return {
      id:          c.id,
      name:        c.name,
      translations,
      imageBase64: c.imageBase64,
      active:      c.active,
      createdAt:   c.createdAt,
      updatedAt:   c.updatedAt,
    };
  }

  private isDiscounted(originalPrice: number, discountedPrice?: number | null): boolean {
    return typeof discountedPrice === 'number' && discountedPrice >= 0 && discountedPrice < originalPrice;
  }
}
