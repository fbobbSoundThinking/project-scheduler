import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface FilterState {
  searchKeyword: string;
  selectedGroups: Set<string>;
  sortDirection: 'asc' | 'desc';
}

@Injectable({
  providedIn: 'root'
})
export class FilterStateService {

  private readonly defaultGroups = new Set([
    'In Progress/Scheduled',
    'Backlog',
    'Pending Authorization',
    'Internal Tracking'
  ]);

  private filterState = new BehaviorSubject<FilterState>({
    searchKeyword: '',
    selectedGroups: new Set(this.defaultGroups),
    sortDirection: 'asc'
  });

  filterState$: Observable<FilterState> = this.filterState.asObservable();

  getFilterState(): FilterState {
    return this.filterState.value;
  }

  updateSearchKeyword(keyword: string): void {
    const current = this.filterState.value;
    this.filterState.next({
      ...current,
      searchKeyword: keyword
    });
  }

  updateSelectedGroups(groups: Set<string>): void {
    const current = this.filterState.value;
    this.filterState.next({
      ...current,
      selectedGroups: new Set(groups)
    });
  }

  updateSortDirection(direction: 'asc' | 'desc'): void {
    const current = this.filterState.value;
    this.filterState.next({
      ...current,
      sortDirection: direction
    });
  }

  updateFilterState(state: Partial<FilterState>): void {
    const current = this.filterState.value;
    this.filterState.next({
      ...current,
      ...state,
      selectedGroups: state.selectedGroups 
        ? new Set(state.selectedGroups) 
        : current.selectedGroups
    });
  }

  resetFilters(): void {
    this.filterState.next({
      searchKeyword: '',
      selectedGroups: new Set(this.defaultGroups),
      sortDirection: 'asc'
    });
  }
}
