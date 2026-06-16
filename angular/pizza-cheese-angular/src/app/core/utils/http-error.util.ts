import { HttpErrorResponse } from '@angular/common/http';

export function getHttpErrorMessage(
  err: HttpErrorResponse,
  fallback: string,
): string {
  if (err.status === 0) {
    return 'Không thể kết nối server. Kiểm tra backend đang chạy và cấu hình CORS.';
  }

  if (typeof err.error?.message === 'string' && err.error.message.trim()) {
    return err.error.message;
  }

  if (typeof err.error?.error === 'string' && err.error.error.trim()) {
    return err.error.error;
  }

  return fallback;
}
