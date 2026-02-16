import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { ProjectService } from '../../../core/services/project.service';
import { FilterStateService } from '../../../core/services/filter-state.service';
import { Project, Subitem } from '../../../core/models';

interface GanttItem {
  id: number;
  name: string;
  startDate: Date | null;
  endDate: Date | null;
  color: string;
  developer?: string;
  projectName?: string;
  subitemName?: string;
  isSubitem?: boolean;
  developerInitials?: string;
  ratio?: number;
}

interface ProjectGroup {
  projectName: string;
  assignments: GanttItem[];
  rowHeight: number; // Dynamic height based on number of assignments
}

@Component({
  selector: 'app-gantt-view',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './gantt-view.html',
  styleUrls: ['./gantt-view.scss']
})
export class GanttView implements OnInit, OnDestroy {
  @ViewChild('ganttChart') ganttChartElement?: ElementRef;
  
  projects: Project[] = [];
  ganttItems: GanttItem[] = [];
  projectGroups: ProjectGroup[] = [];
  loading: boolean = true;
  
  startDate: Date = new Date();
  endDate: Date = new Date();
  months: { name: string; year: number; days: number }[] = [];
  days: number = 0;
  
  // Filtering and sorting
  searchKeyword: string = '';
  selectedGroups: Set<string> = new Set();
  sortDirection: 'asc' | 'desc' = 'asc';
  filtersExpanded: boolean = true;
  
  private filterSubscription?: Subscription;
  
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
    private filterStateService: FilterStateService,
    private cdr: ChangeDetectorRef
  ) {}
  
  ngOnInit(): void {
    console.log('GanttView component initialized');
    
    // Load initial filter state from shared service
    const filterState = this.filterStateService.getFilterState();
    this.searchKeyword = filterState.searchKeyword;
    this.selectedGroups = new Set(filterState.selectedGroups);
    this.sortDirection = filterState.sortDirection;
    
    this.loadProjects();
  }
  
  ngOnDestroy(): void {
    if (this.filterSubscription) {
      this.filterSubscription.unsubscribe();
    }
  }
  
  trackByFn(index: number, item: GanttItem): number {
    return item.id;
  }
  
  scrollToToday(): void {
    if (!this.ganttChartElement) return;
    
    const chartElement = this.ganttChartElement.nativeElement as HTMLElement;
    const totalDays = this.getDaysBetween(this.startDate, this.endDate);
    const today = new Date();
    const offset = this.getDaysBetween(this.startDate, today);
    const scrollPercent = offset / totalDays;
    
    // Scroll to today, centered in view
    const scrollWidth = chartElement.scrollWidth;
    const viewWidth = chartElement.clientWidth;
    const scrollPosition = (scrollWidth * scrollPercent) - (viewWidth / 2);
    
    chartElement.scrollTo({
      left: Math.max(0, scrollPosition),
      behavior: 'smooth'
    });
  }
  
  loadProjects(): void {
    console.log('Loading projects with details...');
    console.log('Initial loading state:', this.loading);
    this.loading = true;
    
    // Use the with-details endpoint to get assignments
    this.projectService.getAllProjectsWithDetails().subscribe({
      next: (projects) => {
        console.log('Projects loaded:', projects.length, 'projects');
        if (projects.length > 0) {
          console.log('Sample project:', projects[0]);
          console.log('Sample project assignments:', projects[0].assignments);
          console.log('Has assignments?', projects[0].assignments && projects[0].assignments.length > 0);
        }
        this.projects = projects;
        
        console.log('Building gantt data...');
        this.buildGanttData();
        console.log('Gantt items built:', this.ganttItems.length, 'items');
        
        if (this.ganttItems.length > 0) {
          console.log('Sample gantt item:', this.ganttItems[0]);
        } else {
          console.warn('No gantt items built! Projects may not have date ranges.');
        }
        
        console.log('Calculating timeline...');
        this.calculateTimeline();
        console.log('Timeline calculated. Months:', this.months.length);
        
        console.log('Setting loading to FALSE');
        this.loading = false;
        console.log('Final loading state:', this.loading);
        
        // Force change detection
        this.cdr.detectChanges();
        console.log('Change detection triggered');
      },
      error: (err) => {
        console.error('Error loading projects:', err);
        this.loading = false;
        this.cdr.detectChanges();
        console.log('Error - loading state:', this.loading);
      }
    });
  }
  
  buildGanttData(): void {
    this.ganttItems = [];
    const projectMap = new Map<string, GanttItem[]>();
    
    console.log('Building gantt from', this.projects.length, 'projects');
    let projectsWithAssignments = 0;
    let assignmentsWithDates = 0;
    let filteredOut = 0;
    
    this.projects.forEach(project => {
      // Apply group filter
      if (!this.selectedGroups.has(project.group || '')) {
        filteredOut++;
        return;
      }
      
      // Apply search filter
      if (this.searchKeyword && !project.projectName?.toLowerCase().includes(this.searchKeyword.toLowerCase())) {
        filteredOut++;
        return;
      }
      
      if (project.assignments && project.assignments.length > 0) {
        projectsWithAssignments++;
        
        project.assignments.forEach(assignment => {
          // Only include assignments that have a start date
          if (assignment.startDate) {
            assignmentsWithDates++;
            
            const firstName = assignment.developer?.firstName || '';
            const lastName = assignment.developer?.lastName || '';
            const initials = `${firstName.charAt(0).toUpperCase()}${lastName.charAt(0).toUpperCase()}`;
            
            const item: GanttItem = {
              id: assignment.assignmentsId || 0,
              name: `${firstName} ${lastName}`,
              startDate: new Date(assignment.startDate),
              endDate: assignment.endDate ? new Date(assignment.endDate) : null,
              color: this.getColorForDeveloper(assignment.developer?.developersId || 0),
              developer: `${firstName} ${lastName}`,
              projectName: project.projectName,
              isSubitem: false,
              developerInitials: `${initials}:${firstName.toUpperCase()}`,
              ratio: assignment.ratio || 0
            };
            
            this.ganttItems.push(item);
            
            // Group by project
            if (!projectMap.has(project.projectName)) {
              projectMap.set(project.projectName, []);
            }
            projectMap.get(project.projectName)?.push(item);
          }
        });
      }
      
      // Process subitem assignments
      if (project.subitems && project.subitems.length > 0) {
        project.subitems.forEach(subitem => {
          if (subitem.assignments && subitem.assignments.length > 0) {
            subitem.assignments.forEach(assignment => {
              if (assignment.startDate) {
                assignmentsWithDates++;
                
                const firstName = assignment.developer?.firstName || '';
                const lastName = assignment.developer?.lastName || '';
                const initials = `${firstName.charAt(0).toUpperCase()}${lastName.charAt(0).toUpperCase()}`;
                
                const item: GanttItem = {
                  id: assignment.assignmentsId || 0,
                  name: `${firstName} ${lastName}`,
                  startDate: new Date(assignment.startDate),
                  endDate: assignment.endDate ? new Date(assignment.endDate) : null,
                  color: this.getColorForSubitem(assignment.developer?.developersId || 0),
                  developer: `${firstName} ${lastName}`,
                  projectName: project.projectName,
                  subitemName: subitem.subitemName,
                  isSubitem: true,
                  developerInitials: `${initials}:${firstName.toUpperCase()}`,
                  ratio: assignment.ratio || 0
                };
                
                this.ganttItems.push(item);
                
                // Group by project (subitems grouped under parent project)
                if (!projectMap.has(project.projectName)) {
                  projectMap.set(project.projectName, []);
                }
                projectMap.get(project.projectName)?.push(item);
              }
            });
          }
        });
      }
    });
    
    // Convert map to array and sort by project name
    this.projectGroups = Array.from(projectMap.entries())
      .map(([projectName, assignments]) => {
        // Calculate row height: 10px top padding + (40px bar height * num assignments) + (10px spacing between bars)
        const barHeight = 24;
        const spacing = 6;
        const topPadding = 6;
        const bottomPadding = 6;
        const rowHeight = topPadding + (assignments.length * barHeight) + ((assignments.length - 1) * spacing) + bottomPadding;
        return { projectName, assignments, rowHeight };
      })
      .sort((a, b) => {
        const comparison = a.projectName.localeCompare(b.projectName);
        return this.sortDirection === 'asc' ? comparison : -comparison;
      });
    
    console.log(`Summary: ${projectsWithAssignments} active projects with assignments, ${assignmentsWithDates} assignments with start dates, ${filteredOut} projects filtered out (Closed/Removed)`);
    console.log(`Grouped into ${this.projectGroups.length} projects`);
    
    // Log projects with multiple developers
    const multiDevProjects = this.projectGroups.filter(g => g.assignments.length > 1);
    console.log(`Projects with multiple developers: ${multiDevProjects.length}`);
    multiDevProjects.slice(0, 3).forEach(g => {
      console.log(`  ${g.projectName}: ${g.assignments.length} developers - ${g.assignments.map(a => a.developerInitials).join(', ')}`);
    });
  }
  
  calculateTimeline(): void {
    const today = new Date();
    console.log('=== calculateTimeline() START ===');
    console.log('Today:', today.toDateString());
    
    // Start 2 months before current month
    this.startDate = new Date(today.getFullYear(), today.getMonth() - 2, 1);
    console.log('Setting startDate to:', this.startDate.toDateString());
    
    // End 13 months from start (2 months before + 12 months + 1 month after = 15 months total)
    this.endDate = new Date(this.startDate.getFullYear(), this.startDate.getMonth() + 15, 0);
    console.log('Setting endDate to:', this.endDate.toDateString());
    
    console.log('Timeline: 2 months before through 1 month after 12-month range:');
    console.log('  Start:', this.startDate.toDateString());
    console.log('  End:', this.endDate.toDateString());
    
    console.log('About to call buildMonthsArray()...');
    this.buildMonthsArray();
    console.log('=== calculateTimeline() END ===');
  }
  
  buildMonthsArray(): void {
    console.log('=== buildMonthsArray() START ===');
    console.log('this.startDate:', this.startDate?.toDateString());
    console.log('this.endDate:', this.endDate?.toDateString());
    
    this.months = [];
    const current = new Date(this.startDate);
    console.log('Starting from:', current.toDateString());
    
    let count = 0;
    while (current <= this.endDate && count < 50) { // Safety limit
      const daysInMonth = new Date(current.getFullYear(), current.getMonth() + 1, 0).getDate();
      this.months.push({
        name: current.toLocaleString('default', { month: 'short' }),
        year: current.getFullYear(),
        days: daysInMonth
      });
      this.days += daysInMonth;
      current.setMonth(current.getMonth() + 1);
      count++;
    }
    
    console.log(`Built ${this.months.length} months from ${this.startDate.toDateString()} to ${this.endDate.toDateString()}`);
    console.log('First month:', this.months[0]);
    console.log('Last month:', this.months[this.months.length - 1]);
    console.log('Total days:', this.days);
    console.log('=== buildMonthsArray() END ===');
  }
  
  getItemPosition(item: GanttItem): { left: string; width: string } {
    if (!item.startDate || !item.endDate) {
      return { left: '0%', width: '0%' };
    }
    
    if (!this.startDate || !this.endDate) {
      return { left: '0%', width: '0%' };
    }
    
    try {
      const totalDays = this.getDaysBetween(this.startDate, this.endDate);
      
      if (totalDays <= 0) {
        console.warn('Invalid timeline range:', this.startDate, this.endDate);
        return { left: '0%', width: '1%' };
      }
      
      let startOffset = this.getDaysBetween(this.startDate, item.startDate);
      let duration = this.getDaysBetween(item.startDate, item.endDate) ;
      if (startOffset < 0) {
        startOffset = 0; // Clamp to 0 if before timeline start
        duration = this.getDaysBetween(this.startDate, item.endDate);
      }
    
      
      const left = (startOffset / totalDays) * 100;
    
      const width = (duration / totalDays) * 100;
      
      console.log('Item:', item.projectName, 'for developer', item.developer);
      console.log('Item Dates:', this.startDate.toDateString(), 'to', this.endDate.toDateString() , ' totalDays:', totalDays);
      console.log('Item Dates:', item.startDate.toDateString(), 'to', item.endDate.toDateString() , ' startOffset:', startOffset, ' duration:', duration);
      console.log(`Calculated position: left=${left}%, width=${width}%`);

      return {
        left: `${Math.max(0, Math.min(100, left))}%`,
        width: `${Math.max(0.5, Math.min(100, width))}%`
      };
    } catch (err) {
      console.error('Error calculating position:', err, item);
      return { left: '0%', width: '1%' };
    }
  }
  
  getDaysBetween(start: Date, end: Date): number {
    const msPerDay = 1000 * 60 * 60 * 24;
    const diffMs = end.getTime() - start.getTime();
    return Math.ceil(diffMs / msPerDay);
  }

  formatDate(date: Date | null): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  
  getColorForDeveloper(devId: number): string {
    const colors = [
      '#2196F3', '#4CAF50', '#FF9800', '#9C27B0', '#F44336',
      '#00BCD4', '#E91E63', '#607D8B', '#795548', '#3F51B5'
    ];
    return colors[devId % colors.length];
  }
  
  getColorForSubitem(devId: number): string {
    // Subitem colors - slightly lighter/different shades to distinguish from project bars
    const colors = [
      '#64B5F6', '#81C784', '#FFB74D', '#BA68C8', '#EF5350',
      '#4DD0E1', '#F06292', '#90A4AE', '#A1887F', '#7986CB'
    ];
    return colors[devId % colors.length];
  }
  
  getTodayPosition(): string {
    try {
      let today = new Date();
      //today = new Date('2026-06-01'); // For testing purposes
      console.log('Today:', today.toDateString());
      console.log('Timeline start:', this.startDate.toDateString());
      console.log('Timeline end:', this.endDate.toDateString());
          
      const totalDays = this.getDaysBetween(this.startDate, this.endDate);
      const offset = this.getDaysBetween(this.startDate, today);
      
      console.log(`Today position: offset=${offset} days, total=${totalDays} days, percent=${(offset/totalDays)*100}%`,'today:', today.toDateString());
      console.log('Start Date:', this.startDate.toDateString(), 'End Date:', this.endDate.toDateString());

      const todayPos = ((offset / totalDays) * ((window.innerWidth-314)/window.innerWidth) * 100) ; // Adjust for sidebar width
      
      return `${Math.max(0, Math.min(100, todayPos))}%`;
      //return `${Math.max(0, (offset / totalDays) * 100)}%`;
    } catch (err) {
      console.error('Error calculating today position:', err);
      return '0%';
    }
  }
  
  isTodayVisible(): boolean {
    try {
      const today = new Date();
      const isVisible = today >= this.startDate && today <= this.endDate;
      console.log(`Today visible? ${isVisible} (${today.toDateString()} between ${this.startDate.toDateString()} and ${this.endDate.toDateString()})`);
      return isVisible;
    } catch (err) {
      console.error('Error checking if today is visible:', err);
      return false;
    }
  }
  
  // Filtering methods
  toggleGroup(group: string): void {
    if (this.selectedGroups.has(group)) {
      this.selectedGroups.delete(group);
    } else {
      this.selectedGroups.add(group);
    }
    this.filterStateService.updateSelectedGroups(this.selectedGroups);
    this.applyFilters();
  }

  isGroupSelected(group: string): boolean {
    return this.selectedGroups.has(group);
  }

  selectAllGroups(): void {
    this.selectedGroups = new Set(this.groups.map(g => g.value));
    this.filterStateService.updateSelectedGroups(this.selectedGroups);
    this.applyFilters();
  }

  clearAllGroups(): void {
    this.selectedGroups.clear();
    this.filterStateService.updateSelectedGroups(this.selectedGroups);
    this.applyFilters();
  }

  onSearchChange(): void {
    this.filterStateService.updateSearchKeyword(this.searchKeyword);
    this.applyFilters();
  }
  
  applyFilters(): void {
    this.buildGanttData();
    this.calculateTimeline();
    this.cdr.detectChanges();
  }
  
  toggleSort(): void {
    this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    this.filterStateService.updateSortDirection(this.sortDirection);
    this.buildGanttData();
    this.cdr.detectChanges();
  }
  
  getSortIcon(): string {
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }
  
  toggleFilters(): void {
    this.filtersExpanded = !this.filtersExpanded;
  }
}
