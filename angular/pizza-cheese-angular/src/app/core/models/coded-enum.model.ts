export interface CodedEnumValue<T extends string = string> {
  code: number;
  name: T;
  label: string;
}

export type ApiEnumField<T extends string> = CodedEnumValue<T> | T;
