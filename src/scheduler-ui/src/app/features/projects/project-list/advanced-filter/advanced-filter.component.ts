import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FilterRule, FilterGroup, FilterCondition, FilterColumn, FILTER_CONDITIONS } from './filter.model';

@Component({
  selector: 'app-advanced-filter',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './advanced-filter.component.html',
  styleUrls: ['./advanced-filter.component.scss']
})
export class AdvancedFilterComponent implements OnInit {
  @Input() columns: FilterColumn[] = [];
  @Input() isOpen = false;
  @Output() isOpenChange = new EventEmitter<boolean>();
  @Output() applyFilters = new EventEmitter<FilterGroup[]>();
  
  filterGroups: FilterGroup[] = [];
  conditions = FILTER_CONDITIONS;
  activeFilterCount = 0;
  
  ngOnInit(): void {
    if (this.filterGroups.length === 0) {
      this.addFilterGroup();
    }
  }
  
  addFilterGroup(): void {
    const group: FilterGroup = {
      id: this.generateId(),
      operator: 'AND',
      rules: []
    };
    this.filterGroups.push(group);
    this.addFilterRule(group);
  }
  
  addFilterRule(group: FilterGroup): void {
    const rule: FilterRule = {
      id: this.generateId(),
      column: this.columns[0]?.id || '',
      condition: 'is',
      values: [],
      textValue: ''
    };
    group.rules.push(rule);
  }
  
  removeFilterGroup(groupId: string): void {
    this.filterGroups = this.filterGroups.filter(g => g.id !== groupId);
    if (this.filterGroups.length === 0) {
      this.addFilterGroup();
    }
  }
  
  removeFilterRule(group: FilterGroup, ruleId: string): void {
    group.rules = group.rules.filter(r => r.id !== ruleId);
    if (group.rules.length === 0) {
      this.removeFilterGroup(group.id);
    }
  }
  
  getColumnOptions(columnId: string): string[] {
    const column = this.columns.find(c => c.id === columnId);
    return column?.options || [];
  }
  
  isSelectColumn(columnId: string): boolean {
    const column = this.columns.find(c => c.id === columnId);
    return column?.type === 'select';
  }
  
  requiresValue(condition: FilterCondition): boolean {
    const conditionDef = this.conditions.find(c => c.value === condition);
    return conditionDef?.requiresValue ?? true;
  }
  
  toggleValue(rule: FilterRule, value: string): void {
    const index = rule.values.indexOf(value);
    if (index > -1) {
      rule.values.splice(index, 1);
    } else {
      rule.values.push(value);
    }
  }
  
  isValueSelected(rule: FilterRule, value: string): boolean {
    return rule.values.includes(value);
  }
  
  onColumnChange(rule: FilterRule): void {
    // Reset values when column changes
    rule.values = [];
    rule.textValue = '';
    
    // Reset to appropriate condition
    const column = this.columns.find(c => c.id === rule.column);
    if (column?.type === 'select') {
      rule.condition = 'is';
    } else {
      rule.condition = 'text_is';
    }
  }
  
  onConditionChange(rule: FilterRule): void {
    // Reset values when condition changes
    if (!this.requiresValue(rule.condition)) {
      rule.values = [];
      rule.textValue = '';
    }
  }
  
  apply(): void {
    // Calculate active filters
    this.activeFilterCount = this.filterGroups.reduce((count, group) => {
      return count + group.rules.filter(r => 
        r.values.length > 0 || 
        r.textValue || 
        !this.requiresValue(r.condition)
      ).length;
    }, 0);
    
    this.applyFilters.emit(this.filterGroups);
    this.close();
  }
  
  clearAll(): void {
    this.filterGroups = [];
    this.addFilterGroup();
    this.activeFilterCount = 0;
    this.applyFilters.emit([]);
  }
  
  close(): void {
    this.isOpen = false;
    this.isOpenChange.emit(false);
  }
  
  private generateId(): string {
    return Math.random().toString(36).substring(2, 9);
  }
  
  getConditionsForColumn(columnId: string): typeof FILTER_CONDITIONS {
    const column = this.columns.find(c => c.id === columnId);
    if (column?.type === 'select') {
      return this.conditions.filter(c => ['is', 'is_not', 'is_empty', 'is_not_empty'].includes(c.value));
    }
    return this.conditions;
  }
}
