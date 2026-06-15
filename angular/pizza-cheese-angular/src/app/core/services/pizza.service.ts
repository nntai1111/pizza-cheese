import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import { PageResponse } from '../models/page.model';
import {
  CreatePizzaRequest,
  Pizza,
  UpdatePizzaRequest,
} from '../models/pizza.model';

export interface PizzaListParams {
  activeOnly?: boolean;
  categoryId?: string | null;
  page?: number;
  size?: number;
}

export interface PizzaImageUpload {
  mainImage?: File | null;
  secondaryImages?: File[];
}

@Injectable({ providedIn: 'root' })
export class PizzaService {
  private readonly http = inject(HttpClient);

  list(params: PizzaListParams = {}): Observable<PageResponse<Pizza>> {
    let httpParams = new HttpParams()
      .set('activeOnly', String(params.activeOnly ?? true))
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 12));

    if (params.categoryId) {
      httpParams = httpParams.set('categoryId', params.categoryId);
    }

    return this.http
      .get<ApiResponse<PageResponse<Pizza>>>(`${API_BASE_URL}/pizzas`, {
        params: httpParams,
      })
      .pipe(map((response) => response.data));
  }

  getById(id: string): Observable<Pizza> {
    return this.http
      .get<ApiResponse<Pizza>>(`${API_BASE_URL}/pizzas/${id}`)
      .pipe(map((response) => response.data));
  }

  create(
    request: CreatePizzaRequest,
    imageUpload?: PizzaImageUpload,
  ): Observable<Pizza> {
    if (this.hasImageUpload(imageUpload)) {
      return this.http
        .post<ApiResponse<Pizza>>(
          `${API_BASE_URL}/admin/pizzas`,
          this.toMultipartBody(request, imageUpload!),
        )
        .pipe(map((response) => response.data));
    }

    return this.http
      .post<ApiResponse<Pizza>>(`${API_BASE_URL}/admin/pizzas`, request)
      .pipe(map((response) => response.data));
  }

  update(
    id: string,
    request: UpdatePizzaRequest,
    imageUpload?: PizzaImageUpload,
  ): Observable<Pizza> {
    if (this.hasImageUpload(imageUpload)) {
      return this.http
        .put<ApiResponse<Pizza>>(
          `${API_BASE_URL}/admin/pizzas/${id}`,
          this.toMultipartBody(request, imageUpload!),
        )
        .pipe(map((response) => response.data));
    }

    return this.http
      .put<ApiResponse<Pizza>>(`${API_BASE_URL}/admin/pizzas/${id}`, request)
      .pipe(map((response) => response.data));
  }

  delete(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<null>>(`${API_BASE_URL}/admin/pizzas/${id}`)
      .pipe(map(() => undefined));
  }

  private hasImageUpload(imageUpload?: PizzaImageUpload): boolean {
    return Boolean(
      imageUpload?.mainImage ||
        (imageUpload?.secondaryImages && imageUpload.secondaryImages.length > 0),
    );
  }

  private toMultipartBody(
    request: CreatePizzaRequest | UpdatePizzaRequest,
    imageUpload: PizzaImageUpload,
  ): FormData {
    const formData = new FormData();
    formData.append(
      'request',
      new Blob([JSON.stringify(request)], { type: 'application/json' }),
    );

    if (imageUpload.mainImage) {
      formData.append(
        'mainImage',
        imageUpload.mainImage,
        imageUpload.mainImage.name,
      );
    }

    imageUpload.secondaryImages?.forEach((file) =>
      formData.append('images', file, file.name),
    );

    return formData;
  }
}
