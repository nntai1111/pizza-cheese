import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { APP_ROLES, AppRole } from '../../../core/enums/role.enum';
import { AuthService } from '../../../core/services/auth.service';
import { UserAvatarComponent } from '../../../shared/components';

interface RoleOption {
  role: AppRole;
  label: string;
  icon: string;
  route: string;
  color: string;
}

@Component({
  selector: 'app-role-select',
  imports: [RouterLink, UserAvatarComponent],
  templateUrl: './role-select.component.html',
  styleUrl: './role-select.component.scss',
})


export class RoleSelectComponent {
  //   ActivatedRoute: lấy thông tin route hiện tại (param, query param, data...)
  // Router: điều hướng trang
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;

  readonly roles: RoleOption[] = APP_ROLES.map((role) => ({
    role,
    label: role,
    icon: this.getRoleIcon(role),
    route: `/welcome/${role.toLowerCase()}`,
    color: this.getRoleColor(role),
  }));

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private getRoleIcon(role: AppRole): string {
    const icons: Record<AppRole, string> = {
      [AppRole.ADMIN]: '👑',
      [AppRole.CUSTOMER]: '🛒',
      [AppRole.CASHIER]: '💰',
      [AppRole.KITCHEN]: '👨‍🍳',
      [AppRole.DELIVERY]: '🛵',
    };
    return icons[role];
  }

  private getRoleColor(role: AppRole): string {
    const colors: Record<AppRole, string> = {
      [AppRole.ADMIN]: '#8b5cf6',
      [AppRole.CUSTOMER]: '#3b82f6',
      [AppRole.CASHIER]: '#10b981',
      [AppRole.KITCHEN]: '#f59e0b',
      [AppRole.DELIVERY]: '#ef4444',
    };
    return colors[role];
  }
}
