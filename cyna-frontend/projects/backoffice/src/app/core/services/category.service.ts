import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface AdminCategory {
  id: string;
  name: string;
  fullName: string;
  fullNameEn: string;
  description: string;
  descriptionEn: string;
  imageBase64: string | null;
  active: boolean;
  productCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryPayload {
  name: string;
  fullName: string;
  fullNameEn: string;
  description: string;
  descriptionEn: string;
}

export interface UpdateCategoryPayload {
  name: string;
  fullName: string;
  fullNameEn: string;
  description: string;
  descriptionEn: string;
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
    return this.http.get<ApiResponse<AdminCategory[]>>(
      `${environment.apiUrl}/categories`,
      { headers: { 'Cache-Control': 'no-cache' } }
    );
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
}
