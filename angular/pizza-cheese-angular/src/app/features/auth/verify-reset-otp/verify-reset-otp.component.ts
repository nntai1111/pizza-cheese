import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-reset-otp',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './verify-reset-otp.component.html',
  styleUrl: './verify-reset-otp.component.scss',
})
export class VerifyResetOtpComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly email = signal('');
  readonly infoMessage = signal<string | null>(null);
  readonly loading = signal(false);
  readonly resendLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  ngOnInit(): void {
    const email =
      this.route.snapshot.queryParamMap.get('email')?.trim().toLowerCase() ??
      '';

    if (!email) {
      void this.router.navigateByUrl('/forgot-password');
      return;
    }

    this.email.set(email);
    const stateMessage = history.state?.['message'] as string | undefined;
    this.infoMessage.set(
      stateMessage ??
        'Nếu email tồn tại trong hệ thống, chúng tôi đã gửi mã OTP. Vui lòng kiểm tra hộp thư.',
    );
  }

  onSubmit(): void {
    if (this.form.invalid || !this.email()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const otp = this.form.controls.otp.value.trim();
    this.authService.verifyResetOtp(this.email(), otp).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigateByUrl('/reset-password');
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.error ??
            err?.error?.message ??
            'Xác thực OTP thất bại. Vui lòng thử lại.',
        );
      },
    });
  }

  resend(): void {
    if (!this.email() || this.resendLoading()) {
      return;
    }

    this.resendLoading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.forgotPassword(this.email()).subscribe({
      next: (data) => {
        this.resendLoading.set(false);
        this.successMessage.set(data.message);
      },
      error: (err) => {
        this.resendLoading.set(false);
        this.errorMessage.set(
          err?.error?.error ??
            err?.error?.message ??
            'Không gửi lại được mã OTP. Vui lòng thử lại.',
        );
      },
    });
  }
}
