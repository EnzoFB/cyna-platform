import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ProductTranslation {
  name: string;
  serviceDescription: string;
  technicalDescription: string;
  highlightPoints: string[];
}

export interface AdminProduct {
  id: string;
  name: string;
  translations: Record<string, ProductTranslation>;
  categoryId: string;
  categoryName: string;
  priorityLevel: number;
  monthlyPrice: number;
  annualPrice: number;
  currency: string;
  primaryImageBase64: string | null;
  isPublished: boolean;
  isAvailable: boolean;
}

export interface AdminProductDetail extends AdminProduct {
  serviceDescription: string;
  technicalDescription: string;
  highlightPoints: string[];
  freeTrialDays: number;
  images: { id: string; base64: string }[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductPayload {
  translations: Record<string, ProductTranslation>;
  categoryId: string;
  priorityLevel: number;
  monthlyPrice: number;
  annualPrice: number;
  currency: string;
  freeTrialDays: number;
}

export interface UpdateProductPayload extends CreateProductPayload {
  isPublished: boolean;
  isAvailable: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
}

export interface PagedData<T> {
  items: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  getProducts(page: number, size: number, filters?: {
    search?: string;
    categoryId?: string;
    published?: boolean;
    available?: boolean;
  }) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters?.search)                    params = params.set('search', filters.search);
    if (filters?.categoryId)                params = params.set('categoryId', filters.categoryId);
    if (filters?.published !== undefined)   params = params.set('published', filters.published);
    if (filters?.available !== undefined)   params = params.set('available', filters.available);
    return this.http.get<ApiResponse<PagedData<any>>>(
      `${environment.apiUrl}/products`,
      { params, headers: { 'Cache-Control': 'no-cache' } }
    ).pipe(map(r => ({
      ...r,
      data: { ...r.data, items: r.data.items.map((dto: any) => this.mapProduct(dto)) }
    } as ApiResponse<PagedData<AdminProduct>>)));
  }

  getProductDetail(id: string) {
    return this.http.get<ApiResponse<any>>(
      `${environment.apiUrl}/products/${id}`,
      { headers: { 'Cache-Control': 'no-cache' } }
    ).pipe(map(r => ({ ...r, data: this.mapProductDetail(r.data) } as ApiResponse<AdminProductDetail>)));
  }

  createProduct(payload: CreateProductPayload) {
    return this.http.post<ApiResponse<string>>(
      `${environment.apiUrl}/products`,
      payload
    );
  }

  updateProduct(id: string, payload: UpdateProductPayload) {
    return this.http.put<ApiResponse<string>>(
      `${environment.apiUrl}/products/${id}`,
      payload
    );
  }

  addProductImage(id: string, file: File) {
    const formData = new FormData();
    formData.append('image', file);
    return this.http.post<ApiResponse<string>>(
      `${environment.apiUrl}/products/${id}/images`,
      formData
    );
  }

  deleteProductImage(productId: string, imageId: string) {
    return this.http.delete<void>(
      `${environment.apiUrl}/products/${productId}/images/${imageId}`
    );
  }

  reorderProductImages(productId: string, orderedImageIds: string[]) {
    return this.http.put<void>(
      `${environment.apiUrl}/products/${productId}/images/order`,
      orderedImageIds
    );
  }

  deleteProduct(id: string) {
    return this.http.delete<void>(`${environment.apiUrl}/products/${id}`);
  }

  bulkDeleteProducts(ids: string[]) {
    return this.http.delete<void>(`${environment.apiUrl}/products/batch`, { body: ids });
  }

  private mapProduct(dto: any): AdminProduct {
    const translations: Record<string, ProductTranslation> = dto.translations ?? {};
    return {
      id:                 dto.id,
      name:               translations['fr']?.name ?? '',
      translations,
      categoryId:         dto.categoryId,
      categoryName:       dto.categoryName,
      priorityLevel:      dto.priorityLevel,
      monthlyPrice:       dto.monthlyPrice,
      annualPrice:        dto.annualPrice,
      currency:           dto.currency,
      primaryImageBase64: dto.primaryImageBase64 ?? null,
      isPublished:        dto.isPublished,
      isAvailable:        dto.isAvailable,
    };
  }

  private mapProductDetail(dto: any): AdminProductDetail {
    const base = this.mapProduct(dto);
    const frT = base.translations['fr'];
    return {
      ...base,
      serviceDescription:   frT?.serviceDescription ?? '',
      technicalDescription:  frT?.technicalDescription ?? '',
      highlightPoints:      frT?.highlightPoints ?? [],
      freeTrialDays:        dto.freeTrialDays ?? 0,
      images:               dto.images ?? [],
      createdAt:            dto.createdAt ?? '',
      updatedAt:            dto.updatedAt ?? '',
    };
  }
}
