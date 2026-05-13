import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface AdminProduct {
  id: string;
  name: string;
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

export interface AdminProductDetail {
  id: string;
  name: string;
  categoryId: string;
  categoryName: string;
  priorityLevel: number;
  serviceDescription: string;
  technicalDescription: string;
  monthlyPrice: number;
  annualPrice: number;
  currency: string;
  isPublished: boolean;
  isAvailable: boolean;
  freeTrialDays: number;
  highlightPoints: string[];
  images: { id: string; base64: string }[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductPayload {
  name: string;
  categoryId: string;
  priorityLevel: number;
  serviceDescription: string;
  technicalDescription: string;
  monthlyPrice: number;
  annualPrice: number;
  currency: string;
  freeTrialDays: number;
  highlightPoints: string[];
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
    return this.http.get<ApiResponse<PagedData<AdminProduct>>>(
      `${environment.apiUrl}/products`,
      { params, headers: { 'Cache-Control': 'no-cache' } }
    );
  }

  getProductDetail(id: string) {
    return this.http.get<ApiResponse<AdminProductDetail>>(
      `${environment.apiUrl}/products/${id}`,
      { headers: { 'Cache-Control': 'no-cache' } }
    );
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
}
