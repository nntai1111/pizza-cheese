import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

import { AppRole } from '../../../core/enums/role.enum';
import { AuthService } from '../../../core/services/auth.service';
import { UserAvatarComponent } from '../../../shared/components';

const WELCOME_MESSAGES: Record<string, { title: string; description: string; icon: string }> = {
  admin: {
    title: 'Chào mừng Admin',
    description: 'Quản lý hệ thống, người dùng và cấu hình Pizza Cheese.',
    icon: '👑',
  },
  customer: {
    title: 'Chào mừng Khách hàng',
    description: 'Đặt pizza yêu thích và theo dõi đơn hàng của bạn.',
    icon: '🛒',
  },
  cashier: {
    title: 'Chào mừng Thu ngân',
    description: 'Xử lý thanh toán và quản lý đơn hàng tại quầy.',
    icon: '💰',
  },
  kitchen: {
    title: 'Chào mừng Bếp',
    description: 'Nhận đơn và chuẩn bị pizza cho khách hàng.',
    icon: '👨‍🍳',
  },
  delivery: {
    title: 'Chào mừng Giao hàng',
    description: 'Nhận đơn giao và cập nhật trạng thái giao pizza.',
    icon: '🛵',
  },
};

@Component({
  selector: 'app-welcome',
  imports: [RouterLink, UserAvatarComponent],
  templateUrl: './welcome.component.html',
  styleUrl: './welcome.component.scss',
})
export class WelcomeComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  readonly user = this.authService.currentUser;

  private readonly roleParam = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('role') ?? '')),
    { initialValue: '' },
  );

  readonly welcomeInfo = computed(() => {
    const role = this.roleParam().toLowerCase();
    return (
      WELCOME_MESSAGES[role] ?? {
        title: 'Chào mừng',
        description: 'Khu vực làm việc Pizza Cheese.',
        icon: '🍕',
      }
    );
  });

  readonly roleLabel = computed(() => this.roleParam().toUpperCase() as AppRole);
}
