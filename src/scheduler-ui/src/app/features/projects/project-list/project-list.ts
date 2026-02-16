import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { ProjectService } from '../../../core/services/project.service';
import { AssignmentService } from '../../../core/services/assignment.service';
import { TeamService } from '../../../core/services/team.service';
import { DeveloperService } from '../../../core/services/developer.service';
import { FilterStateService } from '../../../core/services/filter-state.service';
import { Project, Assignment, Subitem } from '../../../core/models';
import { AdvancedFilterComponent } from './advanced-filter/advanced-filter.component';
import { FilterGroup, FilterColumn } from './advanced-filter/filter.model';

@Component({
  selector: 'app-project-list',
  imports: [CommonModule, RouterModule, FormsModule, AdvancedFilterComponent],
  templateUrl: './project-list.html',
  styleUrl: './project-list.scss',
})
export class ProjectList implements OnInit, OnDestroy {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  searchKeyword: string = '';
  loading: boolean = false;
  syncing: boolean = false;
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Advanced filter
  showAdvancedFilter = false;
  advancedFilters: FilterGroup[] = [];
  filterColumns: FilterColumn[] = [];
  
  private filterSubscription?: Subscription;
  
  // Edit state
  editingAssignmentId: number | null = null;
  editedDates: { [key: number]: { start: string, end: string, ratio: number } } = {};
  savingAssignmentId: number | null = null;
  
  // Delete state
  deletingAssignmentId: number | null = null;
  
  // Add developer state
  addingDeveloperToProjectId: number | null = null;
  availableDevelopers: any[] = [];
  newDeveloperSelection: { [key: number]: number } = {};
  newDeveloperRatio: { [key: number]: number } = {};
  
  // Add developer to subitem state
  addingDeveloperToSubitem: { [key: number]: boolean } = {};
  newSubitemDeveloperSelection: { [key: number]: number } = {};
  newSubitemDeveloperRatio: { [key: number]: number } = {};
  
  // Team mapping and colors
  teamMap: { [key: number]: string } = {};
  teamColorMap: { [key: string]: string } = {};
  
  // Group filtering
  selectedGroups: Set<string> = new Set();
  availableGroups: string[] = [];
  
  // Expandable rows state
  expandedProjectIds: Set<number> = new Set();

  groups = [
    { value: 'In Progress/Scheduled', label: 'In Progress/Scheduled' },
    { value: 'Backlog', label: 'Backlog' },
    { value: 'Pending Authorization', label: 'Pending Authorization' },
    { value: 'Internal Tracking', label: 'Internal Tracking' },
    { value: 'Closed', label: 'Closed' },
    { value: 'Removed/Cancelled/Duplicate', label: 'Removed/Cancelled/Duplicate' }
  ];

  constructor(
    private projectService: ProjectService,
    private assignmentService: AssignmentService,
    private teamService: TeamService,
    private developerService: DeveloperService,
    private filterStateService: FilterStateService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Load initial filter state from shared service
    const filterState = this.filterStateService.getFilterState();
    this.searchKeyword = filterState.searchKeyword;
    this.selectedGroups = new Set(filterState.selectedGroups);
    this.sortDirection = filterState.sortDirection;
    
    this.loadTeams();
    this.loadProjects(); // Will call initializeFilterColumns after data loads
    this.loadDevelopers();
    
    // Prevent scroll events that go beyond table boundaries
    setTimeout(() => {
      const tableScroller = document.querySelector('.table-scroll-container') as HTMLElement;
      if (tableScroller) {
        tableScroller.addEventListener('wheel', this.preventOverScroll.bind(this), { passive: false });
      }
    }, 100);
  }
  
  private preventOverScroll(event: WheelEvent): void {
    const tableScroller = event.currentTarget as HTMLElement;
    const isScrollingRight = event.deltaX > 0;
    const isScrollingLeft = event.deltaX < 0;
    const atRightEdge = tableScroller.scrollLeft + tableScroller.clientWidth >= tableScroller.scrollWidth;
    const atLeftEdge = tableScroller.scrollLeft === 0;
    
    // Block scroll if trying to go beyond edges
    if ((isScrollingRight && atRightEdge) || (isScrollingLeft && atLeftEdge)) {
      event.preventDefault();
    }
  }
  
  ngOnDestroy(): void {
    if (this.filterSubscription) {
      this.filterSubscription.unsubscribe();
    }
    // Clean up event listener
    const tableScroller = document.querySelector('.table-scroll-container') as HTMLElement;
    if (tableScroller) {
      tableScroller.removeEventListener('wheel', this.preventOverScroll.bind(this));
    }
  }
  
  loadDevelopers(): void {
    this.developerService.getAllDevelopers().subscribe({
      next: (developers) => {
        console.log('Loaded developers:', developers.length);
        this.availableDevelopers = developers;
        console.log('availableDevelopers after assignment:', this.availableDevelopers.length);
      },
      error: (err) => console.error('Error loading developers:', err)
    });
  }
  
  loadTeams(): void {
    this.teamService.getAllTeams().subscribe({
      next: (teams) => {
        this.teamMap = {};
        teams.forEach(team => {
          this.teamMap[team.teamId] = team.teamName;
        });
        // Generate color map dynamically
        this.generateTeamColors(teams.map(t => t.teamName));
      },
      error: (err) => console.error('Error loading teams:', err)
    });
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (data) => {
        this.projects = data;
        
        // Extract unique groups from actual data
        const uniqueGroups = new Set(
          data.map(p => p.group).filter((g): g is string => !!g)
        );
        this.availableGroups = Array.from(uniqueGroups).sort();
        
        // Initialize filter columns with actual data
        this.initializeFilterColumns();
        
        this.filterProjects();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading projects:', error);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  filterProjects(): void {
    this.filteredProjects = this.projects.filter(project => {
      const matchesGroup = this.selectedGroups.size === 0 || 
                           this.selectedGroups.has(project.group || '');
      const matchesSearch = !this.searchKeyword || 
        project.projectName?.toLowerCase().includes(this.searchKeyword.toLowerCase());
      const matchesAdvancedFilter = this.applyAdvancedFilters(project);
      
      return matchesGroup && matchesSearch && matchesAdvancedFilter;
    });
    this.cdr.detectChanges();
  }

  toggleGroup(group: string): void {
    if (this.selectedGroups.has(group)) {
      this.selectedGroups.delete(group);
    } else {
      this.selectedGroups.add(group);
    }
    this.filterStateService.updateSelectedGroups(this.selectedGroups);
    this.filterProjects();
  }

  isGroupSelected(group: string): boolean {
    return this.selectedGroups.has(group);
  }

  selectAllGroups(): void {
    this.selectedGroups = new Set(this.availableGroups);
    this.filterStateService.updateSelectedGroups(this.selectedGroups);
    this.filterProjects();
  }

  clearAllGroups(): void {
    this.selectedGroups.clear();
    this.filterStateService.updateSelectedGroups(this.selectedGroups);
    this.filterProjects();
  }

  onSearchChange(): void {
    this.filterStateService.updateSearchKeyword(this.searchKeyword);
    this.filterProjects();
  }

  getTeamName(teamId?: number): string {
    return this.teamMap[teamId || 0] || '-';
  }

  generateTeamColors(teamNames: string[]): void {
    // Predefined color palette for consistency
    const colors = [
      '#2196F3', // Blue
      '#4CAF50', // Green
      '#FF9800', // Orange
      '#9C27B0', // Purple
      '#F44336', // Red
      '#00BCD4', // Cyan
      '#E91E63', // Pink
      '#607D8B', // Blue Grey
      '#795548', // Brown
      '#3F51B5', // Indigo
      '#009688', // Teal
      '#FFC107', // Amber
      '#8BC34A', // Light Green
      '#FF5722', // Deep Orange
      '#673AB7', // Deep Purple
    ];

    teamNames.forEach((teamName, index) => {
      this.teamColorMap[teamName] = colors[index % colors.length];
    });
  }

  getTeamClass(teamId?: number): string {
    const teamName = this.getTeamName(teamId);
    if (teamName === '-') {
      return 'team-none';
    }
    return 'team-dynamic';
  }

  getTeamStyle(teamId?: number): { [key: string]: string } {
    const teamName = this.getTeamName(teamId);
    if (teamName === '-') {
      return {};
    }
    const color = this.teamColorMap[teamName] || '#795548';
    return {
      'background-color': color,
      'color': 'white'
    };
  }

  getPriorityClass(priority?: number): string {
    if (priority === null || priority === undefined) return '';
    if (priority >= 8) return 'priority-high';
    if (priority >= 5) return 'priority-medium';
    return 'priority-low';
  }

  getUrgencyClass(urgency?: number): string {
    if (urgency === null || urgency === undefined) return '';
    if (urgency >= 8) return 'urgency-high';
    if (urgency >= 5) return 'urgency-medium';
    return 'urgency-low';
  }

  getStatusClass(status?: string): string {
    if (!status) return 'status-empty';
    
    // Map status to CSS class based on Monday.com colors
    const statusMap: { [key: string]: string } = {
      'Completed': 'status-completed',
      'Completed (Waiting...)': 'status-completed-waiting',
      'Requirements Gathe...': 'status-requirements',
      'Internal Tracking - In...': 'status-internal-tracking',
      'Scheduled': 'status-scheduled',
      'Pause': 'status-pause',
      'In Progress': 'status-in-progress',
      'Pending Scheduling': 'status-pending-scheduling',
      'Production Pilot': 'status-production-pilot',
      'Hold - NYPD': 'status-hold-nypd',
      'Wait for Production': 'status-wait-production',
      'Waiting on NYPD': 'status-waiting-nypd',
      'UAT': 'status-uat',
      'Duplicate': 'status-duplicate',
      'Pending Authorizati...': 'status-pending-auth',
      'Closed': 'status-closed',
      'Removed/Cancelled': 'status-removed',
      'Removed per Mtg. 2...': 'status-removed-mtg',
      'Hold': 'status-hold',
      'Request forLOE': 'status-request-loe',
      'Not Assessed': 'status-not-assessed',
      'Orig. HD Ticket': 'status-hd-ticket',
      
      // Subitem-specific statuses
      'Done': 'status-done',
      'Not Started': 'status-not-started',
      'Ready for Testing': 'status-ready-testing',
      'Working on it': 'status-working',
      'Post Launch': 'status-post-launch',
      'Ready for Prod': 'status-ready-prod',
      'Pause/ Need Answers': 'status-pause-answers',
      'Need Requirements': 'status-need-requirements',
      'Ready for UAT': 'status-ready-uat',
      'Testing': 'status-testing',
      'BRD in Progress': 'status-brd-progress'
    };
    
    return statusMap[status] || 'status-default';
  }

  sortBy(column: string): void {
    if (this.sortColumn === column) {
      // Toggle direction if same column
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      // New column, default to ascending
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }

    this.filteredProjects.sort((a, b) => {
      let aValue: any;
      let bValue: any;
      let aIsEmpty = false;
      let bIsEmpty = false;

      switch (column) {
        case 'projectName':
          aValue = a.projectName?.toLowerCase() || '';
          bValue = b.projectName?.toLowerCase() || '';
          aIsEmpty = !a.projectName;
          bIsEmpty = !b.projectName;
          break;
        case 'group':
          aValue = a.group?.toLowerCase() || '';
          bValue = b.group?.toLowerCase() || '';
          aIsEmpty = !a.group;
          bIsEmpty = !b.group;
          break;
        case 'status':
          aValue = a.status?.toLowerCase() || '';
          bValue = b.status?.toLowerCase() || '';
          aIsEmpty = !a.status;
          bIsEmpty = !b.status;
          break;
        case 'level':
          aValue = a.level?.toLowerCase() || '';
          bValue = b.level?.toLowerCase() || '';
          aIsEmpty = !a.level;
          bIsEmpty = !b.level;
          break;
        case 'startDate':
          aValue = a.startDate ? new Date(a.startDate).getTime() : 0;
          bValue = b.startDate ? new Date(b.startDate).getTime() : 0;
          aIsEmpty = !a.startDate;
          bIsEmpty = !b.startDate;
          break;
        case 'qaReadyDate':
          aValue = a.qaReadyDate ? new Date(a.qaReadyDate).getTime() : 0;
          bValue = b.qaReadyDate ? new Date(b.qaReadyDate).getTime() : 0;
          aIsEmpty = !a.qaReadyDate;
          bIsEmpty = !b.qaReadyDate;
          break;
        case 'targetProdDate':
          aValue = a.targetProdDate ? new Date(a.targetProdDate).getTime() : 0;
          bValue = b.targetProdDate ? new Date(b.targetProdDate).getTime() : 0;
          aIsEmpty = !a.targetProdDate;
          bIsEmpty = !b.targetProdDate;
          break;
        case 'primaryTeam':
          aValue = this.getTeamName(a.primaryTeamId);
          bValue = this.getTeamName(b.primaryTeamId);
          aIsEmpty = !a.primaryTeamId;
          bIsEmpty = !b.primaryTeamId;
          break;
        case 'priority':
          aValue = a.priority ?? 0;
          bValue = b.priority ?? 0;
          aIsEmpty = a.priority == null || a.priority === 0;
          bIsEmpty = b.priority == null || b.priority === 0;
          break;
        case 'urgency':
          aValue = a.urgency ?? 0;
          bValue = b.urgency ?? 0;
          aIsEmpty = a.urgency == null || a.urgency === 0;
          bIsEmpty = b.urgency == null || b.urgency === 0;
          break;
        case 'devHours':
          aValue = a.devHours ?? 0;
          bValue = b.devHours ?? 0;
          aIsEmpty = a.devHours == null || a.devHours === 0;
          bIsEmpty = b.devHours == null || b.devHours === 0;
          break;
        case 'wfHours':
          aValue = a.wfHours ?? 0;
          bValue = b.wfHours ?? 0;
          aIsEmpty = a.wfHours == null || a.wfHours === 0;
          bIsEmpty = b.wfHours == null || b.wfHours === 0;
          break;
        case 'developers':
          aValue = a.assignments?.length ?? 0;
          bValue = b.assignments?.length ?? 0;
          aIsEmpty = !a.assignments || a.assignments.length === 0;
          bIsEmpty = !b.assignments || b.assignments.length === 0;
          break;
        default:
          return 0;
      }

      // Push empty/null/zero values to the end regardless of sort direction
      if (aIsEmpty && !bIsEmpty) return 1;
      if (!aIsEmpty && bIsEmpty) return -1;
      if (aIsEmpty && bIsEmpty) return 0;

      if (aValue < bValue) {
        return this.sortDirection === 'asc' ? -1 : 1;
      }
      if (aValue > bValue) {
        return this.sortDirection === 'asc' ? 1 : -1;
      }
      return 0;
    });
  }

  getSortIcon(column: string): string {
    if (this.sortColumn !== column) return '⇅';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  getGroupClass(group?: string): string {
    switch(group) {
      case 'In Progress/Scheduled': return 'group-in-progress';
      case 'Backlog': return 'group-backlog';
      case 'Pending Authorization': return 'group-pending';
      case 'Internal Tracking': return 'group-internal';
      case 'Closed': return 'group-closed';
      case 'Removed/Cancelled/Duplicate': return 'group-removed';
      default: return '';
    }
  }

  // Assignment date editing methods
  isEditing(assignmentId?: number): boolean {
    return this.editingAssignmentId === assignmentId;
  }

  isSaving(assignmentId?: number): boolean {
    return this.savingAssignmentId === assignmentId;
  }

  getDeveloperPositionShort(position?: string): string {
    if (!position) return '';
    if (position.toLowerCase().includes('technical lead')) return 'LEAD';
    if (position.toLowerCase().includes('developer')) return 'DEV';
    return position.toUpperCase();
  }

  getRatioPercentage(ratio?: number): number {
    if (ratio === null || ratio === undefined) return 0;
    return Math.round(ratio * 100);
  }

  startEditingDates(assignment: Assignment): void {
    if (!assignment.assignmentsId) return;
    
    this.editingAssignmentId = assignment.assignmentsId;
    
    // Convert ratio to number to match select option values
    const currentRatio = assignment.ratio ? Number(assignment.ratio) : 1.0;
    
    this.editedDates[assignment.assignmentsId] = {
      start: assignment.startDate || '',
      end: assignment.endDate || '',
      ratio: currentRatio
    };
    
    console.log('Started editing assignment:', assignment.assignmentsId, 'with ratio:', currentRatio);
  }

  saveAssignmentDates(assignment: Assignment): void {
    if (!assignment.assignmentsId) return;
    
    const edited = this.editedDates[assignment.assignmentsId];
    const assignmentId = assignment.assignmentsId;
    
    console.log('Saving assignment:', assignmentId, 'Current editing:', this.editingAssignmentId);
    
    this.savingAssignmentId = assignmentId;
    
    // Create updated assignment object with only necessary fields
    const updatedAssignment: any = {
      assignmentsId: assignmentId,
      startDate: edited.start || null,
      endDate: edited.end || null,
      ratio: edited.ratio,
      project: assignment.project ? { projectsId: assignment.project.projectsId } : null,
      developer: assignment.developer ? { developersId: assignment.developer.developersId } : null
    };
    
    this.assignmentService.updateAssignment(
      assignmentId,
      updatedAssignment
    ).subscribe({
      next: (result) => {
        console.log('Update result:', result);
        console.log('Clearing edit state for:', assignmentId);
        
        // Update the assignment in the local data
        if (result) {
          assignment.startDate = result.startDate;
          assignment.endDate = result.endDate;
          assignment.ratio = result.ratio;
        }
        
        // Clear ALL editing state
        this.editingAssignmentId = null;
        this.savingAssignmentId = null;
        delete this.editedDates[assignmentId];
        
        console.log('After clear - editingAssignmentId:', this.editingAssignmentId);
        console.log('After clear - isEditing check:', this.isEditing(assignmentId));
        
        // Force UI update
        this.cdr.detectChanges();
        
        console.log('Assignment updated, edit state cleared');
      },
      error: (err) => {
        console.error('Error updating assignment dates:', err);
        this.savingAssignmentId = null;
        this.editingAssignmentId = null;
        this.cdr.detectChanges();
        alert('Error updating assignment dates. Please try again.');
      }
    });
  }

  cancelEdit(): void {
    this.editingAssignmentId = null;
    this.editedDates = {};
  }

  confirmDeleteAssignment(assignmentId: number, developerName: string): void {
    const confirmed = confirm(`Are you sure you want to remove ${developerName} from this project?`);
    if (confirmed) {
      this.deleteAssignment(assignmentId);
    }
  }

  deleteAssignment(assignmentId: number): void {
    this.deletingAssignmentId = assignmentId;
    
    this.assignmentService.deleteAssignment(assignmentId).subscribe({
      next: () => {
        // Remove assignment from all projects in memory
        this.projects.forEach(project => {
          if (project.assignments) {
            project.assignments = project.assignments.filter(
              a => a.assignmentsId !== assignmentId
            );
          }
        });
        
        // Remove from filtered projects too
        this.filteredProjects.forEach(project => {
          if (project.assignments) {
            project.assignments = project.assignments.filter(
              a => a.assignmentsId !== assignmentId
            );
          }
        });
        
        this.deletingAssignmentId = null;
        this.cdr.detectChanges();
        console.log('Developer removed successfully');
      },
      error: (err) => {
        console.error('Error removing developer:', err);
        this.deletingAssignmentId = null;
        this.cdr.detectChanges();
        alert('Error removing developer from project. Please try again.');
      }
    });
  }

  isDeleting(assignmentId?: number): boolean {
    return this.deletingAssignmentId === assignmentId;
  }

  refreshProjects(): void {
    this.loadTeams();
    this.loadProjects();
  }

  syncFromMonday(): void {
    if (this.syncing) return;
    
    if (!confirm('Are you sure you want to sync from Monday.com? This will update the database with the latest data.')) {
      return;
    }
    
    this.syncing = true;
    const apiUrl = 'http://localhost:8080/api/sync/monday';
    
    this.http.post<any>(apiUrl, {}).subscribe({
      next: (response) => {
        console.log('Sync response:', response);
        this.syncing = false;
        
        if (response.success) {
          alert(`Sync completed!\n\nProjects Updated: ${response.projectsUpdated}\nProjects Inserted: ${response.projectsInserted}\nAssignments Updated: ${response.assignmentsUpdated}\nAssignments Inserted: ${response.assignmentsInserted}`);
          this.loadProjects();
        } else {
          alert(`Sync failed: ${response.error || response.message}`);
        }
        
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Sync error:', err);
        this.syncing = false;
        this.cdr.detectChanges();
        alert('Error syncing from Monday.com. Please try again.');
      }
    });
  }

  isAddingDeveloper(projectId?: number): boolean {
    return this.addingDeveloperToProjectId === projectId;
  }

  startAddingDeveloper(projectId: number): void {
    this.addingDeveloperToProjectId = projectId;
    this.newDeveloperRatio[projectId] = 0.5;
    this.newDeveloperSelection[projectId] = 0;
  }

  cancelAddDeveloper(projectId: number): void {
    this.addingDeveloperToProjectId = null;
    delete this.newDeveloperSelection[projectId];
    delete this.newDeveloperRatio[projectId];
  }

  confirmAddDeveloper(projectId: number): void {
    const developerIdStr = this.newDeveloperSelection[projectId];
    const ratioStr = this.newDeveloperRatio[projectId] || 0.5;

    if (!developerIdStr) {
      alert('Please select a developer');
      return;
    }

    // Convert strings to numbers (select values return strings)
    const developerId = typeof developerIdStr === 'string' ? 
      parseInt(developerIdStr, 10) : developerIdStr;
    const ratio = typeof ratioStr === 'string' ? 
      parseFloat(ratioStr) : ratioStr;

    console.log('Creating assignment:', { projectId, developerId, ratio });

    this.assignmentService.createAssignment(projectId, developerId, ratio).subscribe({
      next: () => {
        console.log('Developer added successfully');
        this.cancelAddDeveloper(projectId);
        this.loadProjects();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error adding developer:', err);
        const errorMsg = err.error || 'Error adding developer to project';
        alert(errorMsg);
        this.cdr.detectChanges();
      }
    });
  }
  
  // Subitem developer management methods
  isAddingDeveloperToSubitem(subitemId?: number): boolean {
    return !!subitemId && !!this.addingDeveloperToSubitem[subitemId];
  }

  startAddingDeveloperToSubitem(subitemId: number): void {
    this.addingDeveloperToSubitem[subitemId] = true;
    this.newSubitemDeveloperSelection[subitemId] = 0;
    this.newSubitemDeveloperRatio[subitemId] = 0.5;  // Default 50%
  }

  cancelAddDeveloperToSubitem(subitemId: number): void {
    delete this.addingDeveloperToSubitem[subitemId];
    delete this.newSubitemDeveloperSelection[subitemId];
    delete this.newSubitemDeveloperRatio[subitemId];
  }

  confirmAddDeveloperToSubitem(subitemId: number): void {
    const developerIdStr = this.newSubitemDeveloperSelection[subitemId];
    const ratioStr = this.newSubitemDeveloperRatio[subitemId] || 0.5;

    if (!developerIdStr) {
      alert('Please select a developer');
      return;
    }

    // Convert strings to numbers (select values return strings)
    const developerId = typeof developerIdStr === 'string' ? 
      parseInt(developerIdStr, 10) : developerIdStr;
    const ratio = typeof ratioStr === 'string' ? 
      parseFloat(ratioStr) : ratioStr;

    console.log('Creating subitem assignment:', { subitemId, developerId, ratio });

    this.assignmentService.createSubitemAssignment(subitemId, developerId, ratio).subscribe({
      next: () => {
        console.log('Developer added to subitem successfully');
        this.cancelAddDeveloperToSubitem(subitemId);
        this.loadProjects();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error adding developer to subitem:', err);
        const errorMsg = err.error || 'Error adding developer to subitem';
        alert(errorMsg);
        delete this.addingDeveloperToSubitem[subitemId];
        this.cdr.detectChanges();
      }
    });
  }
  
  // Advanced Filter Methods
  initializeFilterColumns(): void {
    // Extract unique statuses from actual data
    const uniqueStatuses = new Set(
      this.projects.map(p => p.status).filter((s): s is string => !!s)
    );
    const statusOptions = Array.from(uniqueStatuses).sort();
    
    this.filterColumns = [
      { id: 'projectName', label: 'Project Name', type: 'text' },
      { id: 'group', label: 'Group', type: 'select', options: this.groups.map(g => g.value) },
      { id: 'status', label: 'Status', type: 'select', options: statusOptions },
      { id: 'level', label: 'Level', type: 'text' },
      { id: 'primaryTeam', label: 'Primary Team', type: 'text' }
    ];
  }
  
  openAdvancedFilter(): void {
    this.showAdvancedFilter = true;
  }
  
  onApplyAdvancedFilters(filters: FilterGroup[]): void {
    this.advancedFilters = filters;
    this.filterProjects();
  }
  
  applyAdvancedFilters(project: Project): boolean {
    if (this.advancedFilters.length === 0) {
      return true;
    }
    
    // Evaluate all groups (groups are OR'd together)
    return this.advancedFilters.some(group => this.evaluateFilterGroup(group, project));
  }
  
  private evaluateFilterGroup(group: FilterGroup, project: Project): boolean {
    if (group.rules.length === 0) {
      return true;
    }
    
    // Evaluate rules within group based on operator
    if (group.operator === 'AND') {
      return group.rules.every(rule => this.evaluateFilterRule(rule, project));
    } else {
      return group.rules.some(rule => this.evaluateFilterRule(rule, project));
    }
  }
  
  private evaluateFilterRule(rule: any, project: Project): boolean {
    const fieldValue = this.getFieldValue(project, rule.column);
    
    switch (rule.condition) {
      case 'is':
        return rule.values.includes(fieldValue);
      case 'is_not':
        return !rule.values.includes(fieldValue);
      case 'text_is':
        return fieldValue?.toLowerCase() === rule.textValue?.toLowerCase();
      case 'text_is_not':
        return fieldValue?.toLowerCase() !== rule.textValue?.toLowerCase();
      case 'contains':
        return fieldValue?.toLowerCase().includes(rule.textValue?.toLowerCase() || '');
      case 'does_not_contain':
        return !fieldValue?.toLowerCase().includes(rule.textValue?.toLowerCase() || '');
      case 'starts_with':
        return fieldValue?.toLowerCase().startsWith(rule.textValue?.toLowerCase() || '');
      case 'ends_with':
        return fieldValue?.toLowerCase().endsWith(rule.textValue?.toLowerCase() || '');
      case 'is_empty':
        return !fieldValue || fieldValue.trim() === '';
      case 'is_not_empty':
        return !!fieldValue && fieldValue.trim() !== '';
      default:
        return true;
    }
  }
  
  private getFieldValue(project: Project, fieldName: string): string {
    const value = (project as any)[fieldName];
    return value?.toString() || '';
  }
  
  get activeAdvancedFilterCount(): number {
    return this.advancedFilters.reduce((count, group) => {
      return count + group.rules.filter(r => 
        r.values.length > 0 || r.textValue || ['is_empty', 'is_not_empty'].includes(r.condition)
      ).length;
    }, 0);
  }
  
  // Expandable rows methods
  toggleProjectExpansion(projectId: number): void {
    if (this.expandedProjectIds.has(projectId)) {
      this.expandedProjectIds.delete(projectId);
    } else {
      this.expandedProjectIds.add(projectId);
    }
  }

  isProjectExpanded(projectId: number): boolean {
    return this.expandedProjectIds.has(projectId);
  }

  hasSubitems(project: Project): boolean {
    return project.subitems !== undefined && project.subitems !== null && project.subitems.length > 0;
  }

  getSubitemAssignments(subitem: Subitem): Assignment[] {
    return subitem.assignments || [];
  }
}

