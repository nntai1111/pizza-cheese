import { OrderStatus } from './order.model';
import { LineItemType } from './cart.model';
import { ApiEnumField } from './coded-enum.model';

export interface AdminDashboardKpis {
  ordersCompletedToday: number;
  ordersIncomplete: number;
  collectedToday: number;
  revenueCompletedToday: number;
  customersTotal: number;
  customersNewToday: number;
}

export interface AdminDashboardStatusCount {
  status: ApiEnumField<OrderStatus>;
  count: number;
}

export interface AdminDashboard {
  generatedAt: string;
  kpis: AdminDashboardKpis;
  incompleteByStatus: AdminDashboardStatusCount[];
}

export interface AdminDashboardStatsSummary {
  ordersCreated: number;
  ordersCompleted: number;
  revenueCompleted: number;
  collected: number;
  avgOrderValue: number;
}

export interface AdminDashboardPeriodCompare {
  from: string;
  to: string;
  ordersCompleted: number;
  revenueCompleted: number;
  collected: number;
  revenueChangePercent: number | null;
  collectedChangePercent: number | null;
  ordersChangePercent: number | null;
}

export interface AdminDashboardDailyPoint {
  date: string;
  revenueCompleted: number;
  collected: number;
  ordersCompleted: number;
}

export interface AdminDashboardTopItem {
  name: string;
  itemType: ApiEnumField<LineItemType>;
  quantity: number;
  revenue: number;
}

export interface AdminDashboardStats {
  from: string;
  to: string;
  summary: AdminDashboardStatsSummary;
  previousPeriod: AdminDashboardPeriodCompare;
  series: AdminDashboardDailyPoint[];
  topItems: AdminDashboardTopItem[];
}
