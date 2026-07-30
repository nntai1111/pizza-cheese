import { Component, inject, signal } from '@angular/core';

import { ComboService } from '../../../core/services/combo.service';
import { Combo } from '../../../core/models/combo.model';
import {
  formatComboItemSummary,
  formatComboPrice,
  getComboDiscountedPrice,
  getComboImageUrl,
} from '../../../core/utils/combo.util';
import { formatVnd } from '../../../core/utils/pizza.util';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ComboDetailComponent } from '../combo-detail/combo-detail.component';

@Component({
  selector: 'app-combo-list',
  imports: [PaginationComponent, ComboDetailComponent],
  templateUrl: './combo-list.component.html',
  styleUrl: './combo-list.component.scss',
})
export class ComboListComponent {
  private readonly comboService = inject(ComboService);

  readonly combos = signal<Combo[]>([]);
  readonly selectedComboId = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly pageSize = 12;
  readonly getImageUrl = getComboImageUrl;
  readonly formatPrice = formatComboPrice;
  readonly formatVnd = formatVnd;
  readonly getDiscountedPrice = getComboDiscountedPrice;
  readonly formatItemSummary = formatComboItemSummary;

  constructor() {
    this.loadCombos();
  }

  onPageChange(page: number): void {
    this.page.set(page);
    this.loadCombos();
  }

  viewDetail(comboId: string): void {
    this.selectedComboId.set(comboId);
  }

  closeDetail(): void {
    this.selectedComboId.set(null);
  }

  private loadCombos(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.comboService
      .list({
        activeOnly: true,
        page: this.page(),
        size: this.pageSize,
      })
      .subscribe({
        next: (result) => {
          this.combos.set(result.content);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.errorMessage.set('Không thể tải danh sách combo.');
          this.loading.set(false);
        },
      });
  }
}
