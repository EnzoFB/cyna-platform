import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CategoryTranslation {
  fullName: string;
  description: string;
}

export interface AdminCategory {
  id: string;
  name: string;
  fullName: string;
  description: string;
  translations: Record<string, CategoryTranslation>;
  imageBase64: string | null;
  active: boolean;
  productCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryPayload {
  name: string;
  translations: Record<string, CategoryTranslation>;
}

export interface UpdateCategoryPayload {
  name: string;
  translations: Record<string, CategoryTranslation>;
  active: boolean;
}

interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);

  getCategories() {
    return this.http.get<ApiResponse<any[]>>(
      `${environment.apiUrl}/categories`,
      { headers: { 'Cache-Control': 'no-cache' } }
    ).pipe(map(r => ({ ...r, data: r.data.map((dto: any) => this.mapCategory(dto)) } as ApiResponse<AdminCategory[]>)));
  }

  createCategory(payload: CreateCategoryPayload) {
    return this.http.post<ApiResponse<string>>(
      `${environment.apiUrl}/categories`,
      payload
    );
  }

  updateCategory(id: string, payload: UpdateCategoryPayload) {
    return this.http.put<ApiResponse<string>>(
      `${environment.apiUrl}/categories/${id}`,
      payload
    );
  }

  uploadCategoryImage(id: string, file: File) {
    const formData = new FormData();
    formData.append('image', file);
    return this.http.patch<void>(
      `${environment.apiUrl}/categories/${id}/image`,
      formData
    );
  }

  deleteCategory(id: string) {
    return this.http.delete<void>(`${environment.apiUrl}/categories/${id}`);
  }

  private mapCategory(dto: any): AdminCategory {
    const translations: Record<string, CategoryTranslation> = dto.translations ?? {};
    return {
      id:           dto.id,
      name:         dto.name,
      fullName:     translations['fr']?.fullName ?? '',
      description:  translations['fr']?.description ?? '',
      translations,
      imageBase64:  dto.imageBase64 ?? null,
      active:       dto.active,
      productCount: dto.productCount ?? 0,
      createdAt:    dto.createdAt ?? '',
      updatedAt:    dto.updatedAt ?? '',
    };
  }
}
