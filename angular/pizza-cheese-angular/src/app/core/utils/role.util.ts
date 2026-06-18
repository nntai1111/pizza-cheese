import { AppRole } from '../enums/role.enum';
import { User } from '../models/auth.model';

export function userHasRole(user: User | null, role: AppRole): boolean {
  return user?.roles?.includes(role) ?? false;
}

export function getDefaultRouteForUser(user: User | null): string {
  if (!user?.roles?.length) {
    return '/dashboard';
  }

  if (user.roles.length === 1) {
    return getRouteForRole(user.roles[0] as AppRole);
  }

  return '/dashboard';
}

export function getRouteForRole(role: AppRole): string {
  switch (role) {
    case AppRole.ADMIN:
      return '/admin/categories';
    case AppRole.CUSTOMER:
      return '/customer/pizzas';
    case AppRole.CASHIER:
      return '/cashier/pizzas';
    default:
      return `/welcome/${role.toLowerCase()}`;
  }
}
