import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AdminService } from '../../../core/services/admin.service';
import {
  AdminDashboard,
  AdminDashboardDailyPoint,
  AdminDashboardStats,
  AdminDashboardStatusCount,
  AdminDashboardTopItem,
} from '../../../core/models/admin-dashboard.model';
import { ORDER_STATUS_LABELS } from '../../../core/models/order.model';
import { formatVnd } from '../../../core/utils/pizza.util';
import { getHttpErrorMessage } from '../../../core/utils/http-error.util';
import { codedEnumName, enumEquals, getEnumLabel } from '../../../core/utils/coded-enum.util';
import { orderStatusTone, statusBadgeClass } from '../../../core/utils/status-tone.util';

function toIsoDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function daysAgoIso(days: number): string {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - days);
  return toIsoDate(date);
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [DatePipe, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent {
  private readonly adminService = inject(AdminService);
  private readonly router = inject(Router);

  readonly data = signal<AdminDashboard | null>(null);
  readonly stats = signal<AdminDashboardStats | null>(null);
  readonly loading = signal(true);
  readonly statsLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly statsError = signal<string | null>(null);
  readonly fromDate = signal(daysAgoIso(6));
  readonly toDate = signal(toIsoDate(new Date()));

  readonly formatPrice = formatVnd;
  readonly incompleteStatusLabel = (item: AdminDashboardStatusCount) =>
    getEnumLabel(item.status, ORDER_STATUS_LABELS);
  readonly incompleteBadgeClass = (item: AdminDashboardStatusCount) =>
    statusBadgeClass(orderStatusTone(item.status));

  readonly seriesMax = computed(() => {
    const points = this.stats()?.series ?? [];
    let max = 0;
    for (const point of points) {
      max = Math.max(max, Number(point.revenueCompleted) || 0, Number(point.collected) || 0);
    }
    return max || 1;
  });

  constructor() {
    this.reload();
  }

  openIncompleteOrders(item?: AdminDashboardStatusCount): void {
    const status = item ? codedEnumName(item.status) : null;
    this.router.navigate(['/admin/orders'], {
      queryParams: status ? { status } : {},
    });
  }

  setRangePreset(days: number): void {
    this.fromDate.set(daysAgoIso(days - 1));
    this.toDate.set(toIsoDate(new Date()));
    this.loadStats();
  }

  applyStatsRange(): void {
    this.loadStats();
  }

  barHeight(value: number): string {
    const pct = Math.max(2, Math.round((Number(value) / this.seriesMax()) * 100));
    return `${pct}%`;
  }

  dayLabel(point: AdminDashboardDailyPoint): string {
    const parts = point.date.split('-');
    return parts.length === 3 ? `${parts[2]}/${parts[1]}` : point.date;
  }

  itemTypeLabel(item: AdminDashboardTopItem): string {
    return enumEquals(item.itemType, 'COMBO') ? 'Combo' : 'Pizza';
  }

  changeClass(value: number | null | undefined): string {
    if (value == null) {
      return 'change change--flat';
    }
    if (value > 0) {
      return 'change change--up';
    }
    if (value < 0) {
      return 'change change--down';
    }
    return 'change change--flat';
  }

  formatChange(value: number | null | undefined): string {
    if (value == null) {
      return '—';
    }
    const sign = value > 0 ? '+' : '';
    return `${sign}${value}%`;
  }

  formatPeriod(from: string, to: string): string {
    return `${this.shortDate(from)} – ${this.shortDate(to)}`;
  }

  reload(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.adminService.getDashboard().subscribe({
      next: (dashboard) => {
        this.data.set(dashboard);
        this.loading.set(false);
        this.loadStats();
      },
      error: (err: HttpErrorResponse) => {
        this.errorMessage.set(getHttpErrorMessage(err, 'Không tải được dashboard'));
        this.loading.set(false);
      },
    });
  }

  private shortDate(iso: string): string {
    const parts = iso.split('-');
    return parts.length === 3 ? `${parts[2]}/${parts[1]}` : iso;
  }

  private loadStats(): void {
    this.statsLoading.set(true);
    this.statsError.set(null);
    this.adminService
      .getDashboardStats({
        from: this.fromDate() || undefined,
        to: this.toDate() || undefined,
      })
      .subscribe({
        next: (result) => {
          this.stats.set(result);
          this.statsLoading.set(false);
        },
        error: (err: HttpErrorResponse) => {
          this.statsError.set(getHttpErrorMessage(err, 'Không tải được thống kê'));
          this.statsLoading.set(false);
        },
      });
  }
}
