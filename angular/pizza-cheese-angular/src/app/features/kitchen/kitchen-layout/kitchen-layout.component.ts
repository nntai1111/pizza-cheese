import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { UserAvatarComponent } from '../../../shared/components';

@Component({
  selector: 'app-kitchen-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UserAvatarComponent],
  templateUrl: './kitchen-layout.component.html',
  styleUrl: './kitchen-layout.component.scss',
})
export class KitchenLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly currentYear = new Date().getFullYear();

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
