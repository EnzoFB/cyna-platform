import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';

import { Product, ProductDetail } from '../models/product.model';
import { Category } from '../models/category.model';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PagedResponse } from '../../../core/models/api-response.model';

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
      .get<ApiResponse<PagedResponse<Product>>>(`${environment.apiUrl}/products`, { params: queryParams })
      .pipe(map(response => response.data));
  }

  getCategories(): Observable<Category[]> {
    return this.http
      .get<ApiResponse<Category[]>>(`${environment.apiUrl}/categories`)
      .pipe(map(response => response.data));
  }

  getProductById(productId: string): Observable<ProductDetail | null> {
    return this.http
      .get<ApiResponse<ProductDetail>>(`${environment.apiUrl}/products/${productId}`)
      .pipe(map(response => response.data));
  }
}
