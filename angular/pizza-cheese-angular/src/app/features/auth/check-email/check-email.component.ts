import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-check-email',
  imports: [RouterLink],
  templateUrl: './check-email.component.html',
  styleUrl: './check-email.component.scss',
})
export class CheckEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly email = signal('');
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  ngOnInit(): void {
    const email =
      this.route.snapshot.queryParamMap.get('email')?.trim() ??
      (history.state?.['email'] as string | undefined)?.trim() ??
      '';

    if (!email) {
      void this.router.navigateByUrl('/register');
      return;
    }

    this.email.set(email);
  }

  resend(): void {
    if (!this.email() || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.resendVerification(this.email()).subscribe({
      next: (data) => {
        this.loading.set(false);
        this.successMessage.set(data.message);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.error ??
            err?.error?.message ??
            'Không gửi lại được email. Vui lòng thử lại.',
        );
      },
    });
  }
}
