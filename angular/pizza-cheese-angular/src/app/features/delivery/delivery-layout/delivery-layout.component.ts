import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { UserAvatarComponent } from '../../../shared/components';

@Component({
  selector: 'app-delivery-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, UserAvatarComponent],
  templateUrl: './delivery-layout.component.html',
  styleUrl: './delivery-layout.component.scss',
})
export class DeliveryLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly currentYear = new Date().getFullYear();

  logout(): void {
    this.authService.logout();
    void this.router.navigate(['/login']);
  }
}
