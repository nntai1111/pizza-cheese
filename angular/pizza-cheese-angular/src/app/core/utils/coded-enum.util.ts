import { ApiEnumField, CodedEnumValue } from '../models/coded-enum.model';

export function codedEnumName<T extends string>(
  value: ApiEnumField<T> | null | undefined,
): T | null {
  if (value == null) {
    return null;
  }
  if (typeof value === 'string') {
    return value;
  }
  return value.name;
}

export function codedEnumLabel<T extends string>(
  value: ApiEnumField<T> | null | undefined,
): string {
  if (value == null) {
    return '';
  }
  if (typeof value === 'string') {
    return value;
  }
  return value.label;
}

export function codedEnumCode(
  value: ApiEnumField<string> | null | undefined,
): number | null {
  if (value == null) {
    return null;
  }
  if (typeof value === 'string') {
    return null;
  }
  return value.code;
}

export function isCodedEnumValue(
  value: ApiEnumField<string> | null | undefined,
): value is CodedEnumValue {
  return typeof value === 'object' && value !== null && 'name' in value;
}

export function normalizeCodedEnum<T extends string>(
  value: ApiEnumField<T> | null | undefined,
): T | null {
  return codedEnumName(value);
}

export function normalizeCodedEnumList<T extends string>(
  values: ApiEnumField<T>[] | null | undefined,
): T[] {
  return (values ?? [])
    .map((value) => normalizeCodedEnum(value))
    .filter((value): value is T => value != null);
}

export function enumEquals<T extends string>(
  value: ApiEnumField<T> | null | undefined,
  expected: T,
): boolean {
  return normalizeCodedEnum(value) === expected;
}

export function codedEnumSame<T extends string>(
  a: ApiEnumField<T> | null | undefined,
  b: ApiEnumField<T> | null | undefined,
): boolean {
  return normalizeCodedEnum(a) === normalizeCodedEnum(b);
}

export function getEnumLabel<T extends string>(
  value: ApiEnumField<T> | null | undefined,
  labels: Record<T, string>,
): string {
  if (value == null) {
    return '';
  }
  if (typeof value === 'object') {
    return value.label;
  }
  return labels[value];
}
