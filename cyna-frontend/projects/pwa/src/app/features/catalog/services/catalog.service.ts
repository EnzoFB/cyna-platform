import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { Product, ProductDetail } from '../models/product.model';
import { Category } from '../models/category.model';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PagedResponse } from '../../../core/models/api-response.model';

interface ProductApiDto {
  readonly id: string;
  readonly name: string;
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
  readonly serviceDescription: string;
  readonly technicalDescription: string;
  readonly freeTrialDays: number;
  readonly highlightPoints: readonly string[];
  readonly images: readonly { id: string; base64: string }[];
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
      .get<ApiResponse<Category[]>>(`${environment.apiUrl}/categories`)
      .pipe(map(response => response.data));
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

  private mapProductDetail(product: ProductDetailApiDto | null | undefined): ProductDetail | null {
    if (!product) {
      return null;
    }

    return {
      ...this.mapProduct(product),
      serviceDescription: product.serviceDescription,
      technicalDescription: product.technicalDescription,
      freeTrialDays: product.freeTrialDays,
      highlightPoints: product.highlightPoints,
      images: product.images
    };
  }

  private mapProduct(product: ProductApiDto): Product {
    const hasMonthlyPromotion = this.isDiscounted(product.monthlyPrice, product.discountedMonthlyPrice);
    const hasAnnualPromotion = this.isDiscounted(product.annualPrice, product.discountedAnnualPrice);

    return {
      id: product.id,
      name: product.name,
      categoryId: product.categoryId,
      categoryName: product.categoryName,
      priorityLevel: product.priorityLevel,
      monthlyPrice: hasMonthlyPromotion ? (product.discountedMonthlyPrice as number) : product.monthlyPrice,
      annualPrice: hasAnnualPromotion ? (product.discountedAnnualPrice as number) : product.annualPrice,
      originalMonthlyPrice: hasMonthlyPromotion ? product.monthlyPrice : null,
      originalAnnualPrice: hasAnnualPromotion ? product.annualPrice : null,
      promotionDiscountPercent: product.promotionDiscountPercent ?? null,
      currency: product.currency,
      primaryImageBase64: product.primaryImageBase64,
      isPublished: product.isPublished,
      isAvailable: product.isAvailable
    };
  }

  private isDiscounted(originalPrice: number, discountedPrice?: number | null): boolean {
    return typeof discountedPrice === 'number' && discountedPrice >= 0 && discountedPrice < originalPrice;
  }
}
