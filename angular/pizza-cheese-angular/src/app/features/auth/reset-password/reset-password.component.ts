import { Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

function passwordMatchValidator(
  group: AbstractControl,
): ValidationErrors | null {
  const password = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  if (!password || !confirm) {
    return null;
  }
  return password === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  private resetToken = '';

  readonly form = this.fb.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordMatchValidator },
  );

  ngOnInit(): void {
    const token = this.authService.getPasswordResetToken();
    if (!token) {
      void this.router.navigateByUrl('/forgot-password');
      return;
    }
    this.resetToken = token;
  }

  onSubmit(): void {
    if (this.form.invalid || !this.resetToken) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const { newPassword } = this.form.getRawValue();
    this.authService.resetPassword(this.resetToken, newPassword).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate(['/login'], {
          queryParams: { reset: '1' },
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.error ??
            err?.error?.message ??
            'Đặt lại mật khẩu thất bại. Vui lòng thử lại.',
        );
      },
    });
  }
}
