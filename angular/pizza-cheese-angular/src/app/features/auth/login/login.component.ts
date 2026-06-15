import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { getDefaultRouteForUser } from '../../../core/utils/role.util';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    login: ['user', [Validators.required]],
    password: ['123456', [Validators.required, Validators.minLength(6)]],
  });

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
          err?.error?.message ?? 'Đăng nhập thất bại. Vui lòng thử lại.';
        this.errorMessage.set(message);
      },
    });
  }
}
