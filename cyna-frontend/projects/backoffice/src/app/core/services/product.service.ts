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
  primaryImageUrl: string | null;
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
  imageUrls: string[];
  createdAt: string;
  updatedAt: string;
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

  getProducts(page: number, size: number, search?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
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

  deleteProduct(id: string) {
    return this.http.delete<void>(`${environment.apiUrl}/products/${id}`);
  }
}
