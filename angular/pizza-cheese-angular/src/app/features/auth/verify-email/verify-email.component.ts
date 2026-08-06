import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  imports: [RouterLink],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss',
})
export class VerifyEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly loading = signal(true);
  readonly success = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token')?.trim() ?? '';

    if (!token) {
      this.loading.set(false);
      this.errorMessage.set('Link xác thực không hợp lệ. Vui lòng đăng ký lại.');
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        void this.router.navigate(['/login'], {
          queryParams: { verified: '1' },
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.error ??
            err?.error?.message ??
            'Xác thực thất bại. Vui lòng đăng ký lại.',
        );
      },
    });
  }
}
