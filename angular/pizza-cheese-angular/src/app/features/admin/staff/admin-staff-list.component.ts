import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import { ToastService } from '../../../core/services/toast.service';
import { User } from '../../../core/models/auth.model';
import { AppRole } from '../../../core/enums/role.enum';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { codedEnumName } from '../../../core/utils/coded-enum.util';
import {
  fieldErrorMessage,
  markFormInvalidAndMessage,
  showFieldError,
} from '../../../core/utils/form-validation.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

type RoleFilter = AppRole | 'ALL';

const STAFF_FIELD_LABELS: Record<string, string> = {
  username: 'Username',
  email: 'Email',
  password: 'Mật khẩu',
  fullName: 'Họ tên',
  phone: 'Số điện thoại',
  role: 'Vai trò',
};

const STAFF_ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Quản trị',
  CASHIER: 'Thu ngân',
  KITCHEN: 'Bếp',
  DELIVERY: 'Giao hàng',
};

@Component({
  selector: 'app-admin-staff-list',
  imports: [ReactiveFormsModule, DatePipe, PaginationComponent],
  templateUrl: './admin-staff-list.component.html',
  styleUrl: './admin-staff-list.component.scss',
})
export class AdminStaffListComponent {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);
  private readonly toast = inject(ToastService);

  readonly staff = signal<User[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly roleFilter = signal<RoleFilter>('ALL');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  readonly pageSize = 10;
  readonly staffRoles: AppRole[] = [AppRole.CASHIER, AppRole.KITCHEN, AppRole.DELIVERY, AppRole.ADMIN];
  readonly roleFilters: { value: RoleFilter; label: string }[] = [
    { value: 'ALL', label: 'Tất cả' },
    { value: AppRole.CASHIER, label: 'Thu ngân' },
    { value: AppRole.KITCHEN, label: 'Bếp' },
    { value: AppRole.DELIVERY, label: 'Giao hàng' },
    { value: AppRole.ADMIN, label: 'Admin' },
  ];

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    fullName: ['', Validators.required],
    phone: ['', [Validators.required, Validators.pattern(/^(0|\+84)[0-9]{9,10}$/)]],
    role: [AppRole.CASHIER as AppRole, Validators.required],
    active: [true],
  });

  constructor() {
    this.reload();
  }

  showError(name: string): boolean {
    return showFieldError(this.form.get(name));
  }

  errorOf(name: string): string | null {
    return fieldErrorMessage(this.form.get(name), this.messagesFor(name));
  }

  controlClass(name: string): string {
    return this.showError(name) ? 'is-invalid' : '';
  }

  roleLabel(user: User): string {
    const name = codedEnumName(user.roles?.[0]) ?? '';
    return STAFF_ROLE_LABELS[name] ?? (name || '—');
  }

  setRoleFilter(filter: RoleFilter): void {
    this.roleFilter.set(filter);
    this.page.set(0);
    this.reload();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.reload();
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form.reset({
      username: '',
      email: '',
      password: '',
      fullName: '',
      phone: '',
      role: AppRole.CASHIER,
      active: true,
    });
    this.form.controls.username.enable();
    this.form.controls.email.enable();
    this.form.controls.password.setValidators([Validators.required, Validators.minLength(6)]);
    this.form.controls.password.updateValueAndValidity();
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  openEdit(user: User): void {
    this.editingId.set(user.id);
    const roleName = (codedEnumName(user.roles?.[0]) as AppRole) || AppRole.CASHIER;
    this.form.reset({
      username: user.username,
      email: user.email,
      password: '',
      fullName: user.name,
      phone: user.phone ?? '',
      role: roleName,
      active: user.active !== false,
    });
    this.form.controls.username.disable();
    this.form.controls.email.disable();
    this.form.controls.password.clearValidators();
    this.form.controls.password.setValidators([Validators.minLength(6)]);
    this.form.controls.password.updateValueAndValidity();
    this.showForm.set(true);
    this.errorMessage.set(null);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.toast.error(markFormInvalidAndMessage(this.form, STAFF_FIELD_LABELS));
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);
    const value = this.form.getRawValue();
    const id = this.editingId();

    if (id) {
      this.adminService
        .updateStaff(id, {
          fullName: value.fullName.trim(),
          phone: value.phone.trim(),
          role: value.role,
          active: value.active,
          password: value.password.trim() || undefined,
        })
        .subscribe({
          next: () => {
            this.saving.set(false);
            this.showForm.set(false);
            this.reload();
            this.toast.success('Đã cập nhật nhân viên');
          },
          error: (err: HttpErrorResponse) => {
            this.saving.set(false);
            const message = getHttpErrorMessage(err, 'Cập nhật nhân viên thất bại.');
            this.errorMessage.set(message);
            this.toast.error(message);
          },
        });
      return;
    }

    this.adminService
      .createStaff({
        username: value.username.trim(),
        email: value.email.trim(),
        password: value.password,
        fullName: value.fullName.trim(),
        phone: value.phone.trim(),
        role: value.role,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.showForm.set(false);
          this.reload();
          this.toast.success('Đã tạo nhân viên');
        },
        error: (err: HttpErrorResponse) => {
          this.saving.set(false);
          const message = getHttpErrorMessage(err, 'Tạo nhân viên thất bại.');
          this.errorMessage.set(message);
          this.toast.error(message);
        },
      });
  }

  toggleActive(user: User): void {
    const next = user.active === false;
    const action = next ? 'mở khóa' : 'khóa';
    if (!confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} tài khoản "${user.username}"?`)) {
      return;
    }

    this.adminService.setStaffActive(user.id, next).subscribe({
      next: () => this.reload(),
      error: (err: HttpErrorResponse) => {
        alert(getHttpErrorMessage(err, `Không thể ${action} tài khoản.`));
      },
    });
  }

  private messagesFor(name: string): Partial<Record<string, string>> {
    if (name === 'username') {
      return {
        required: 'Vui lòng nhập username',
        minlength: 'Username tối thiểu 3 ký tự',
      };
    }
    if (name === 'email') {
      return {
        required: 'Vui lòng nhập email',
        email: 'Email không hợp lệ',
      };
    }
    if (name === 'password') {
      return {
        required: 'Vui lòng nhập mật khẩu',
        minlength: 'Mật khẩu tối thiểu 6 ký tự',
      };
    }
    if (name === 'fullName') {
      return { required: 'Vui lòng nhập họ tên' };
    }
    if (name === 'phone') {
      return {
        required: 'Vui lòng nhập số điện thoại',
        pattern: 'Số điện thoại không hợp lệ (VD: 09xxxxxxxx)',
      };
    }
    if (name === 'role') {
      return { required: 'Vui lòng chọn vai trò' };
    }
    return {};
  }

  private reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const role = this.roleFilter();
    this.adminService
      .getStaff({
        role: role === 'ALL' ? undefined : role,
        page: this.page(),
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          this.staff.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(getHttpErrorMessage(err, 'Không thể tải danh sách nhân viên.'));
        },
      });
  }
}
