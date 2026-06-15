import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.scss',
})
export class PaginationComponent {
  readonly page = input(0);
  readonly totalPages = input(0);
  readonly totalElements = input(0);
  readonly disabled = input(false);

  readonly pageChange = output<number>();

  readonly hasPrev = computed(() => this.page() > 0);
  readonly hasNext = computed(() => this.page() < this.totalPages() - 1);
  readonly displayPage = computed(() => this.page() + 1);

  goPrev(): void {
    if (this.hasPrev() && !this.disabled()) {
      this.pageChange.emit(this.page() - 1);
    }
  }

  goNext(): void {
    if (this.hasNext() && !this.disabled()) {
      this.pageChange.emit(this.page() + 1);
    }
  }
}
