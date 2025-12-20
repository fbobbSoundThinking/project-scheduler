import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../../../core/services/project.service';
import { AssignmentService } from '../../../core/services/assignment.service';
import { Project, Assignment } from '../../../core/models';

@Component({
  selector: 'app-project-list',
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './project-list.html',
  styleUrl: './project-list.scss',
})
export class ProjectList implements OnInit {
  projects: Project[] = [];
  filteredProjects: Project[] = [];
  selectedStatus: string = 'all';
  searchKeyword: string = '';
  loading: boolean = false;
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Edit state
  editingAssignmentId: number | null = null;
  editedDates: { [key: number]: { start: string, end: string } } = {};
  savingAssignmentId: number | null = null;

  statuses = [
    { value: 'all', label: 'All Projects' },
    { value: 'In Progress', label: 'In Progress' },
    { value: 'Backlog', label: 'Backlog' },
    { value: 'Pending Authorization', label: 'Pending Authorization' },
    { value: 'Internal Tracking', label: 'Internal Tracking' }
  ];

  constructor(
    private projectService: ProjectService,
    private assignmentService: AssignmentService
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  loadProjects(): void {
    this.loading = true;
    this.projectService.getAllProjects().subscribe({
      next: (data) => {
        this.projects = data;
        this.filterProjects();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading projects:', error);
        this.loading = false;
      }
    });
  }

  filterProjects(): void {
    this.filteredProjects = this.projects.filter(project => {
      const matchesStatus = this.selectedStatus === 'all' || project.status === this.selectedStatus;
      const matchesSearch = !this.searchKeyword || 
        project.projectName?.toLowerCase().includes(this.searchKeyword.toLowerCase());
      return matchesStatus && matchesSearch;
    });
  }

  onStatusChange(): void {
    this.filterProjects();
  }

  onSearchChange(): void {
    this.filterProjects();
  }

  getTeamName(teamId?: number): string {
    const teamMap: { [key: number]: string } = {
      1: 'TEJAS',
      2: 'CUST',
      3: 'GARVIT',
      4: 'OJASVII',
      5: 'SAANT',
      6: 'UNASSIGNED',
      0: 'UNASSIGNED'
    };
    return teamMap[teamId || 0] || '-';
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

      switch (column) {
        case 'projectName':
          aValue = a.projectName?.toLowerCase() || '';
          bValue = b.projectName?.toLowerCase() || '';
          break;
        case 'status':
          aValue = a.status?.toLowerCase() || '';
          bValue = b.status?.toLowerCase() || '';
          break;
        case 'level':
          aValue = a.level?.toLowerCase() || '';
          bValue = b.level?.toLowerCase() || '';
          break;
        case 'startDate':
          aValue = a.startDate ? new Date(a.startDate).getTime() : 0;
          bValue = b.startDate ? new Date(b.startDate).getTime() : 0;
          break;
        case 'qaReadyDate':
          aValue = a.qaReadyDate ? new Date(a.qaReadyDate).getTime() : 0;
          bValue = b.qaReadyDate ? new Date(b.qaReadyDate).getTime() : 0;
          break;
        case 'targetProdDate':
          aValue = a.targetProdDate ? new Date(a.targetProdDate).getTime() : 0;
          bValue = b.targetProdDate ? new Date(b.targetProdDate).getTime() : 0;
          break;
        case 'primaryTeam':
          aValue = this.getTeamName(a.primaryTeamId);
          bValue = this.getTeamName(b.primaryTeamId);
          break;
        case 'priority':
          aValue = a.priority ?? -1;
          bValue = b.priority ?? -1;
          break;
        case 'urgency':
          aValue = a.urgency ?? -1;
          bValue = b.urgency ?? -1;
          break;
        case 'devHours':
          aValue = a.devHours ?? 0;
          bValue = b.devHours ?? 0;
          break;
        case 'wfHours':
          aValue = a.wfHours ?? 0;
          bValue = b.wfHours ?? 0;
          break;
        case 'developers':
          aValue = a.assignments?.length ?? 0;
          bValue = b.assignments?.length ?? 0;
          break;
        default:
          return 0;
      }

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

  getStatusClass(status?: string): string {
    switch(status) {
      case 'In Progress': return 'status-in-progress';
      case 'Backlog': return 'status-backlog';
      case 'Pending Authorization': return 'status-pending';
      case 'Internal Tracking': return 'status-internal';
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

  startEditingDates(assignment: Assignment): void {
    if (!assignment.assignmentsId) return;
    
    this.editingAssignmentId = assignment.assignmentsId;
    this.editedDates[assignment.assignmentsId] = {
      start: assignment.startDate || '',
      end: assignment.endDate || ''
    };
  }

  saveAssignmentDates(assignment: Assignment): void {
    if (!assignment.assignmentsId) return;
    
    const edited = this.editedDates[assignment.assignmentsId];
    this.savingAssignmentId = assignment.assignmentsId;
    
    // Create updated assignment object with only necessary fields
    const updatedAssignment: any = {
      assignmentsId: assignment.assignmentsId,
      startDate: edited.start || null,
      endDate: edited.end || null,
      ratio: assignment.ratio,
      project: assignment.project ? { projectsId: assignment.project.projectsId } : null,
      developer: assignment.developer ? { developersId: assignment.developer.developersId } : null
    };
    
    this.assignmentService.updateAssignment(
      assignment.assignmentsId,
      updatedAssignment
    ).subscribe({
      next: (result) => {
        // Update the assignment in the local data
        assignment.startDate = result.startDate;
        assignment.endDate = result.endDate;
        
        this.editingAssignmentId = null;
        this.savingAssignmentId = null;
        delete this.editedDates[assignment.assignmentsId!];
        
        console.log('Assignment dates updated successfully');
      },
      error: (err) => {
        console.error('Error updating assignment dates:', err);
        this.savingAssignmentId = null;
        alert('Error updating assignment dates. Please try again.');
      }
    });
  }

  cancelEdit(): void {
    this.editingAssignmentId = null;
    this.editedDates = {};
  }
}
