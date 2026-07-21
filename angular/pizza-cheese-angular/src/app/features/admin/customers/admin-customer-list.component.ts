import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { User } from '../../../core/models/auth.model';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { UserAvatarComponent } from '../../../shared/components';

@Component({
  selector: 'app-admin-customer-list',
  imports: [DatePipe, RouterLink, PaginationComponent, UserAvatarComponent],
  templateUrl: './admin-customer-list.component.html',
  styleUrl: './admin-customer-list.component.scss',
})
export class AdminCustomerListComponent {
  private readonly adminService = inject(AdminService);

  readonly customers = signal<User[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 10;

  constructor() {
    this.reload();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminService
      .getCustomers({ page: this.page(), size: this.pageSize })
      .subscribe({
        next: (result) => {
          this.customers.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải danh sách khách hàng.'));
        },
      });
  }
}
