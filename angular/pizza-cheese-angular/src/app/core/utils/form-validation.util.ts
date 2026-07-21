import { AbstractControl, FormArray, FormGroup } from '@angular/forms';

export function showFieldError(control: AbstractControl | null | undefined): boolean {
  return !!control && control.invalid && (control.touched || control.dirty);
}

export function fieldErrorMessage(
  control: AbstractControl | null | undefined,
  messages: Partial<Record<string, string>> = {},
): string | null {
  if (!showFieldError(control) || !control?.errors) {
    return null;
  }

  const errors = control.errors;
  if (errors['required']) {
    return messages['required'] ?? 'Vui lòng nhập thông tin này';
  }
  if (errors['email']) {
    return messages['email'] ?? 'Email không hợp lệ';
  }
  if (errors['minlength']) {
    return (
      messages['minlength'] ??
      `Tối thiểu ${errors['minlength'].requiredLength} ký tự`
    );
  }
  if (errors['maxlength']) {
    return (
      messages['maxlength'] ??
      `Tối đa ${errors['maxlength'].requiredLength} ký tự`
    );
  }
  if (errors['min']) {
    return messages['min'] ?? `Giá trị tối thiểu là ${errors['min'].min}`;
  }
  if (errors['max']) {
    return messages['max'] ?? `Giá trị tối đa là ${errors['max'].max}`;
  }
  if (errors['pattern']) {
    return messages['pattern'] ?? 'Định dạng không hợp lệ';
  }
  return messages['default'] ?? 'Giá trị không hợp lệ';
}

/** Returns first invalid field label for toast, or null. */
export function firstInvalidFieldLabel(
  form: FormGroup | FormArray,
  labels: Record<string, string>,
  prefix = '',
): string | null {
  for (const [key, control] of Object.entries(form.controls)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (control instanceof FormGroup || control instanceof FormArray) {
      const nested = firstInvalidFieldLabel(control, labels, path);
      if (nested) {
        return nested;
      }
      continue;
    }
    if (control.invalid) {
      return (
        labels[path] ??
        labels[key] ??
        (path.startsWith('items.') || key === 'items' ? labels['items'] : undefined) ??
        key
      );
    }
  }
  return null;
}

export function markFormInvalidAndMessage(
  form: FormGroup,
  labels: Record<string, string>,
): string {
  form.markAllAsTouched();
  const label = firstInvalidFieldLabel(form, labels);
  return label
    ? `Vui lòng kiểm tra: ${label}`
    : 'Vui lòng điền đủ thông tin bắt buộc';
}
