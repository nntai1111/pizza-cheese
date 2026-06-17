import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { API_BASE_URL } from '../constants/api.constants';
import { ApiResponse } from '../models/auth.model';
import {
  Combo,
  CreateComboRequest,
  UpdateComboRequest,
} from '../models/combo.model';
import { PageResponse } from '../models/page.model';

export interface ComboListParams {
  activeOnly?: boolean;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ComboService {
  private readonly http = inject(HttpClient);

  list(params: ComboListParams = {}): Observable<PageResponse<Combo>> {
    const httpParams = new HttpParams()
      .set('activeOnly', String(params.activeOnly ?? true))
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 12));

    return this.http
      .get<ApiResponse<PageResponse<Combo>>>(`${API_BASE_URL}/combos`, {
        params: httpParams,
      })
      .pipe(map((response) => response.data));
  }

  getById(id: string): Observable<Combo> {
    return this.http
      .get<ApiResponse<Combo>>(`${API_BASE_URL}/combos/${id}`)
      .pipe(map((response) => response.data));
  }

  create(
    request: CreateComboRequest,
    imageFile?: File | null,
  ): Observable<Combo> {
    if (imageFile) {
      return this.http
        .post<ApiResponse<Combo>>(
          `${API_BASE_URL}/admin/combos`,
          this.toMultipartBody(request, imageFile),
        )
        .pipe(map((response) => response.data));
    }

    return this.http
      .post<ApiResponse<Combo>>(`${API_BASE_URL}/admin/combos`, request)
      .pipe(map((response) => response.data));
  }

  update(
    id: string,
    request: UpdateComboRequest,
    imageFile?: File | null,
  ): Observable<Combo> {
    if (imageFile) {
      return this.http
        .put<ApiResponse<Combo>>(
          `${API_BASE_URL}/admin/combos/${id}`,
          this.toMultipartBody(request, imageFile),
        )
        .pipe(map((response) => response.data));
    }

    return this.http
      .put<ApiResponse<Combo>>(`${API_BASE_URL}/admin/combos/${id}`, request)
      .pipe(map((response) => response.data));
  }

  delete(id: string): Observable<void> {
    return this.http
      .delete<ApiResponse<null>>(`${API_BASE_URL}/admin/combos/${id}`)
      .pipe(map(() => undefined));
  }

  private toMultipartBody(
    request: CreateComboRequest | UpdateComboRequest,
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
