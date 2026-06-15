import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { DEFAULT_AVATAR_URL } from '../../../core/constants/api.constants';
import { AuthService } from '../../../core/services/auth.service';

const ALLOWED_AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_AVATAR_SIZE = 2 * 1024 * 1024;

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly avatarPreview = signal(DEFAULT_AVATAR_URL);
  readonly avatarFile = signal<File | null>(null);
  readonly avatarError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[a-zA-Z0-9_]+$/),
      ],
    ],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: [
      '',
      [
        Validators.required,
        Validators.pattern(/^(0|\+84)[0-9]{9,10}$/),
      ],
    ],
  });

  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    if (!ALLOWED_AVATAR_TYPES.includes(file.type)) {
      this.avatarError.set('Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP');
      return;
    }

    if (file.size > MAX_AVATAR_SIZE) {
      this.avatarError.set('Ảnh không được vượt quá 2MB');
      return;
    }

    this.avatarError.set(null);
    this.avatarFile.set(file);

    const reader = new FileReader();
    reader.onload = () => {
      this.avatarPreview.set(reader.result as string);
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService
      .register(this.form.getRawValue(), this.avatarFile())
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading.set(false);
          const message =
            err?.error?.error ??
            err?.error?.message ??
            'Đăng ký thất bại. Vui lòng thử lại.';
          this.errorMessage.set(message);
        },
      });
  }
}
