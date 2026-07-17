export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  /** Server clock — send back as updatedSince on next poll. */
  syncedAt?: string | null;
  /** true = content is only changed orders since updatedSince. */
  incremental?: boolean;
}

