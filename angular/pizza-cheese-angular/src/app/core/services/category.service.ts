import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import {
  Category,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '../models/category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);

  list(activeOnly = true): Observable<Category[]> {
    const params = new HttpParams().set('activeOnly', String(activeOnly));
    return this.http
      .get<ApiResponse<Category[]>>(`${API_BASE_URL}/categories`, { params })
      .pipe(map((response) => response.data));
  }

  create(
    request: CreateCategoryRequest,
    imageFile?: File | null,
  ): Observable<Category> {
    if (imageFile) {
      return this.http
        .post<ApiResponse<Category>>(
          `${API_BASE_URL}/admin/categories`,
          this.toMultipartBody(request, imageFile),
        )
        .pipe(map((response) => response.data));
    }

    return this.http
      .post<ApiResponse<Category>>(`${API_BASE_URL}/admin/categories`, request)
      .pipe(map((response) => response.data));
  }

  update(
    id: string,
    request: UpdateCategoryRequest,
    imageFile?: File | null,
  ): Observable<Category> {
    if (imageFile) {
      return this.http
        .put<ApiResponse<Category>>(
          `${API_BASE_URL}/admin/categories/${id}`,
          this.toMultipartBody(request, imageFile),
        )
        .pipe(map((response) => response.data));
    }

    return this.http
      .put<ApiResponse<Category>>(
        `${API_BASE_URL}/admin/categories/${id}`,
        request,
      )
      .pipe(map((response) => response.data));
  }

  delete(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<null>>(`${API_BASE_URL}/admin/categories/${id}`)
      .pipe(map(() => undefined));
  }

  private toMultipartBody(
    request: CreateCategoryRequest | UpdateCategoryRequest,
    imageFile: File,
  ): FormData {
    const formData = new FormData();
    formData.append(
      'request',
      new Blob([JSON.stringify(request)], { type: 'application/json' }),
    );
    formData.append('image', imageFile, imageFile.name);
    return formData;
  }
}
