import { Component, computed, inject, input } from '@angular/core';

import { AuthService } from '../../../core/services/auth.service';
import { User } from '../../../core/models/auth.model';

@Component({
  selector: 'app-user-avatar',
  template: `
    <img
      class="user-avatar"
      [class.user-avatar--sm]="size() === 'sm'"
      [class.user-avatar--md]="size() === 'md'"
      [class.user-avatar--lg]="size() === 'lg'"
      [src]="avatarUrl()"
      [alt]="alt()"
    />
  `,
  styles: `
    .user-avatar {
      border-radius: 50%;
      object-fit: cover;
      flex-shrink: 0;
      background: #e5e7eb;
    }

    .user-avatar--sm {
      width: 40px;
      height: 40px;
    }

    .user-avatar--md {
      width: 64px;
      height: 64px;
    }

    .user-avatar--lg {
      width: 96px;
      height: 96px;
    }
  `,
})
export class UserAvatarComponent {
  private readonly authService = inject(AuthService);

  readonly user = input<User | null | undefined>(null);
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly alt = input('Ảnh đại diện');

  readonly avatarUrl = computed(() =>
    this.authService.getAvatarUrl(this.user()),
  );
}
