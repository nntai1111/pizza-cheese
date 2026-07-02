import { AppRole } from '../enums/role.enum';
import { User } from '../models/auth.model';
import { normalizeCodedEnum, normalizeCodedEnumList } from './coded-enum.util';

export function userHasRole(user: User | null, role: AppRole): boolean {
  return normalizeCodedEnumList(user?.roles).includes(role);
}

export function getDefaultRouteForUser(user: User | null): string {
  if (!user?.roles?.length) {
    return '/dashboard';
  }

  const roles = normalizeCodedEnumList(user.roles);
  if (roles.length === 1) {
    return getRouteForRole(roles[0]);
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
