import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface AdminUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  role: string;
  status: string;
  totalPurchases: number;
  orderCount: number;
  address: string;
  createdAt: string;
}

export interface CreateUserPayload {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface UpdateUserPayload {
  firstName: string;
  lastName: string;
  role: string;
  status: string;
}

export interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  getUsers(page: number = 0, size: number = 10) {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<PagedResponse<AdminUser>>>(
      `${environment.apiUrl}/admin/users`,
      { params }
    );
  }

  createUser(payload: CreateUserPayload) {
    return this.http.post<ApiResponse<{ id: string }>>(
      `${environment.apiUrl}/admin/users`,
      payload
    );
  }

  updateUser(id: string, payload: UpdateUserPayload) {
    return this.http.put<ApiResponse<void>>(
      `${environment.apiUrl}/admin/users/${id}`,
      payload
    );
  }

  deleteUser(id: string) {
    return this.http.delete<ApiResponse<void>>(
      `${environment.apiUrl}/admin/users/${id}`
    );
  }
}
