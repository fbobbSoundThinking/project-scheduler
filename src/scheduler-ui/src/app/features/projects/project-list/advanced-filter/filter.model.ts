export interface FilterRule {
  id: string;
  column: string;
  condition: FilterCondition;
  values: string[];
  textValue?: string;
}

export interface FilterGroup {
  id: string;
  operator: 'AND' | 'OR';
  rules: FilterRule[];
}

export type FilterCondition = 
  | 'is' 
  | 'is_not' 
  | 'text_is' 
  | 'text_is_not' 
  | 'contains' 
  | 'does_not_contain' 
  | 'starts_with'
  | 'ends_with'
  | 'is_empty'
  | 'is_not_empty';

export interface FilterColumn {
  id: string;
  label: string;
  type: 'select' | 'text' | 'date' | 'number';
  options?: string[];
}

export const FILTER_CONDITIONS: { value: FilterCondition; label: string; requiresValue: boolean }[] = [
  { value: 'is', label: 'is', requiresValue: true },
  { value: 'is_not', label: 'is not', requiresValue: true },
  { value: 'text_is', label: 'text is', requiresValue: true },
  { value: 'text_is_not', label: 'text is not', requiresValue: true },
  { value: 'contains', label: 'contains', requiresValue: true },
  { value: 'does_not_contain', label: "doesn't contain", requiresValue: true },
  { value: 'starts_with', label: 'starts with', requiresValue: true },
  { value: 'ends_with', label: 'ends with', requiresValue: true },
  { value: 'is_empty', label: 'is empty', requiresValue: false },
  { value: 'is_not_empty', label: 'is not empty', requiresValue: false }
];
