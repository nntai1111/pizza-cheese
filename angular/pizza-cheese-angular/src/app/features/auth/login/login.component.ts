import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { getDefaultRouteForUser } from '../../../core/utils/role.util';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    login: ['user', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('verified') === '1') {
      this.successMessage.set(
        'Xác thực email thành công. Vui lòng đăng nhập.',
      );
    }
    if (this.route.snapshot.queryParamMap.get('reset') === '1') {
      this.successMessage.set(
        'Đặt lại mật khẩu thành công. Vui lòng đăng nhập.',
      );
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const { login, password } = this.form.getRawValue();
    this.authService.login({ login: login.trim(), password }).subscribe({
      next: (data) => {
        this.loading.set(false);
        this.router.navigateByUrl(getDefaultRouteForUser(data.user));
      },
      error: (err) => {
        this.loading.set(false);
        const message =
          err?.error?.error ??
          err?.error?.message ??
          'Đăng nhập thất bại. Vui lòng thử lại.';
        this.errorMessage.set(message);
      },
    });
  }
}
