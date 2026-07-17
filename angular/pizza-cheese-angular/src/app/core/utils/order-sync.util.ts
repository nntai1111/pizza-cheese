import { enumEquals } from './coded-enum.util';
import { Order, OrderStatus } from '../models/order.model';

export interface OrderMergeOptions {
  /** Current staff user id — required for owned statuses / ALL board. */
  myStaffId?: string | null;
  /** Statuses that only the claiming staff may see. */
  ownedStatuses?: OrderStatus[];
  /** Read owner id from order (deliveryStaffId or kitchenStaffId). */
  getOwnerId?: (order: Order) => string | null | undefined;
  /** Shared pool status visible to everyone (READY / CONFIRMED). */
  sharedStatus?: OrderStatus;
}

/**
 * Merge incremental order changes into the current list for a status filter.
 * - Matches filter (+ ownership when needed) → upsert
 * - No longer matches → remove
 */
export function mergeOrderChanges(
  current: Order[],
  changes: Order[],
  statusFilter: OrderStatus | undefined,
  maxSize?: number,
  options?: OrderMergeOptions,
): Order[] {
  const list = [...current];
  const ownedStatuses = options?.ownedStatuses ?? [];
  const myStaffId = options?.myStaffId ?? null;
  const getOwnerId = options?.getOwnerId;
  const sharedStatus = options?.sharedStatus;

  const isVisible = (order: Order): boolean => {
    if (statusFilter) {
      if (!enumEquals(order.status, statusFilter)) {
        return false;
      }
      if (myStaffId && ownedStatuses.some((status) => status === statusFilter)) {
        return getOwnerId?.(order) === myStaffId;
      }
      return true;
    }

    // ALL: shared pool + only my owned orders
    if (sharedStatus && enumEquals(order.status, sharedStatus)) {
      return true;
    }
    if (myStaffId && getOwnerId && ownedStatuses.some((status) => enumEquals(order.status, status))) {
      return getOwnerId(order) === myStaffId;
    }
    return !ownedStatuses.length;
  };

  for (const change of changes) {
    const index = list.findIndex((order) => order.id === change.id);
    const matches = isVisible(change);

    if (matches) {
      if (index >= 0) {
        list[index] = change;
      } else {
        list.unshift(change);
      }
    } else if (index >= 0) {
      list.splice(index, 1);
    }
  }

  if (maxSize != null && list.length > maxSize) {
    return list.slice(0, maxSize);
  }

  return list;
}
