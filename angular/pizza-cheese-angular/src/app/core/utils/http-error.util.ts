import { HttpErrorResponse } from '@angular/common/http';

export function getHttpErrorMessage(
  err: HttpErrorResponse,
  fallback: string,
): string {
  if (err.status === 0) {
    return 'Không thể kết nối server. Kiểm tra backend đang chạy và cấu hình CORS.';
  }

  const body = err.error;
  // Backend RestResponse: message = short status, error = chi tiết
  if (typeof body?.error === 'string' && body.error.trim()) {
    return body.error;
  }

  if (typeof body?.message === 'string' && body.message.trim()) {
    return body.message;
  }

  return fallback;
}
