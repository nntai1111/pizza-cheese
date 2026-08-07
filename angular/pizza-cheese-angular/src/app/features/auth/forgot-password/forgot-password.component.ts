import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const email = this.form.controls.email.value.trim().toLowerCase();
    this.authService.forgotPassword(email).subscribe({
      next: (data) => {
        this.loading.set(false);
        void this.router.navigate(['/verify-reset-otp'], {
          queryParams: { email: data.email },
          state: { message: data.message },
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.error ??
            err?.error?.message ??
            'Không gửi được mã OTP. Vui lòng thử lại.',
        );
      },
    });
  }
}
