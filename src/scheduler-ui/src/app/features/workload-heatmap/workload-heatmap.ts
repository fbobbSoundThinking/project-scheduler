import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProjectService } from '../../core/services/project.service';
import { Project, Subitem } from '../../core/models';

interface DeveloperWorkload {
  developerId: number;
  developerName: string;
  position: string;
  weeklyLoad: Map<string, number>; // week key -> hours allocated
  overloadedWeeks: Set<string>;
  underutilizedWeeks: Set<string>;
  averageLoad: number;
}

interface WeekInfo {
  weekKey: string;
  weekLabel: string;
  startDate: Date;
  endDate: Date;
}

@Component({
  selector: 'app-workload-heatmap',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './workload-heatmap.html',
  styleUrls: ['./workload-heatmap.scss']
})
export class WorkloadHeatmap implements OnInit {
  developers: DeveloperWorkload[] = [];
  weeks: WeekInfo[] = [];
  loading: boolean = true;
  
  // Constants
  readonly HOURS_PER_DAY = 8;
  readonly DAYS_PER_WEEK = 5;
  readonly CAPACITY_PERCENTAGE = 0.8;
  readonly MAX_HOURS_PER_WEEK = this.HOURS_PER_DAY * this.DAYS_PER_WEEK * this.CAPACITY_PERCENTAGE; // 32 hours

  constructor(
    private projectService: ProjectService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadWorkloadData();
  }

  loadWorkloadData(): void {
    this.loading = true;
    
    this.projectService.getAllProjectsWithDetails().subscribe({
      next: (projects) => {
        this.calculateWorkload(projects);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading projects:', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  calculateWorkload(projects: Project[]): void {
    const developerMap = new Map<number, DeveloperWorkload>();
    const weeksSet = new Set<string>();
    
    // Calculate start and end dates (3 months before and 6 months after today)
    const today = new Date();
    const startDate = new Date(today);
    startDate.setMonth(startDate.getMonth() - 3);
    const endDate = new Date(today);
    endDate.setMonth(endDate.getMonth() + 6);
    
    // Process all assignments
    projects.forEach(project => {
      // Process project-level assignments
      if (project.assignments) {
        project.assignments.forEach(assignment => {
          if (!assignment.startDate || !assignment.developer) return;
          
          const devId = assignment.developer.developersId!;
          const devName = `${assignment.developer.firstName} ${assignment.developer.lastName}`;
          const position = assignment.developer.position || 'Developer';
          const ratio = assignment.ratio || 1.0;
          
          // Initialize developer if not exists
          if (!developerMap.has(devId)) {
            developerMap.set(devId, {
              developerId: devId,
              developerName: devName,
              position: position,
              weeklyLoad: new Map(),
              overloadedWeeks: new Set(),
              underutilizedWeeks: new Set(),
              averageLoad: 0
            });
          }
          
          const dev = developerMap.get(devId)!;
          
          // Calculate weeks for this assignment
          const assignmentStart = new Date(assignment.startDate);
          const assignmentEnd = assignment.endDate ? new Date(assignment.endDate) : new Date(assignmentStart);
          assignmentEnd.setMonth(assignmentEnd.getMonth() + 3); // Default to 3 months if no end date
          
          // Iterate through weeks
          let currentWeekStart = this.getWeekStart(assignmentStart);
          
          while (currentWeekStart <= assignmentEnd && currentWeekStart <= endDate) {
            if (currentWeekStart >= startDate) {
              const weekKey = this.getWeekKey(currentWeekStart);
              weeksSet.add(weekKey);
              
              // Calculate hours for this week
              const hoursThisWeek = this.MAX_HOURS_PER_WEEK * ratio;
              const currentHours = dev.weeklyLoad.get(weekKey) || 0;
              dev.weeklyLoad.set(weekKey, currentHours + hoursThisWeek);
            }
            
            // Move to next week
            currentWeekStart = new Date(currentWeekStart);
            currentWeekStart.setDate(currentWeekStart.getDate() + 7);
          }
        });
      }
      
      // Process subitem assignments
      if (project.subitems) {
        project.subitems.forEach(subitem => {
          if (!subitem.assignments) return;
          
          subitem.assignments.forEach(assignment => {
            if (!assignment.startDate || !assignment.developer) return;
            
            const devId = assignment.developer.developersId!;
            const devName = `${assignment.developer.firstName} ${assignment.developer.lastName}`;
            const position = assignment.developer.position || 'Developer';
            const ratio = assignment.ratio || 1.0;
            
            // Initialize developer if not exists
            if (!developerMap.has(devId)) {
              developerMap.set(devId, {
                developerId: devId,
                developerName: devName,
                position: position,
                weeklyLoad: new Map(),
                overloadedWeeks: new Set(),
                underutilizedWeeks: new Set(),
                averageLoad: 0
              });
            }
            
            const dev = developerMap.get(devId)!;
            
            // Calculate weeks for this assignment
            const assignmentStart = new Date(assignment.startDate);
            const assignmentEnd = assignment.endDate ? new Date(assignment.endDate) : new Date(assignmentStart);
            assignmentEnd.setMonth(assignmentEnd.getMonth() + 3); // Default to 3 months if no end date
            
            // Iterate through weeks
            let currentWeekStart = this.getWeekStart(assignmentStart);
            
            while (currentWeekStart <= assignmentEnd && currentWeekStart <= endDate) {
              if (currentWeekStart >= startDate) {
                const weekKey = this.getWeekKey(currentWeekStart);
                weeksSet.add(weekKey);
                
                // Calculate hours for this week
                const hoursThisWeek = this.MAX_HOURS_PER_WEEK * ratio;
                const currentHours = dev.weeklyLoad.get(weekKey) || 0;
                dev.weeklyLoad.set(weekKey, currentHours + hoursThisWeek);
              }
              
              // Move to next week
              currentWeekStart = new Date(currentWeekStart);
              currentWeekStart.setDate(currentWeekStart.getDate() + 7);
            }
          });
        });
      }
    });
    
    // Generate sorted weeks list
    const sortedWeeks = Array.from(weeksSet).sort();
    this.weeks = sortedWeeks.map(weekKey => {
      const weekStart = this.parseWeekKey(weekKey);
      const weekEnd = new Date(weekStart);
      weekEnd.setDate(weekEnd.getDate() + 6);
      
      return {
        weekKey,
        weekLabel: this.formatWeekLabel(weekStart),
        startDate: weekStart,
        endDate: weekEnd
      };
    });
    
    // Calculate statistics and identify over/under utilized weeks
    developerMap.forEach(dev => {
      let totalHours = 0;
      let weekCount = 0;
      
      this.weeks.forEach(week => {
        const hours = dev.weeklyLoad.get(week.weekKey) || 0;
        totalHours += hours;
        weekCount++;
        
        if (hours > this.MAX_HOURS_PER_WEEK) {
          dev.overloadedWeeks.add(week.weekKey);
        } else if (hours < this.MAX_HOURS_PER_WEEK * 0.5 && hours > 0) {
          dev.underutilizedWeeks.add(week.weekKey);
        }
      });
      
      dev.averageLoad = weekCount > 0 ? totalHours / weekCount : 0;
    });
    
    // Convert to array and sort by name
    this.developers = Array.from(developerMap.values())
      .sort((a, b) => a.developerName.localeCompare(b.developerName));
  }

  getWeekStart(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust to Monday
    return new Date(d.setDate(diff));
  }

  getWeekKey(date: Date): string {
    const year = date.getFullYear();
    const weekNum = this.getWeekNumber(date);
    return `${year}-W${String(weekNum).padStart(2, '0')}`;
  }

  parseWeekKey(weekKey: string): Date {
    const [year, week] = weekKey.split('-W');
    const simple = new Date(parseInt(year), 0, 1 + (parseInt(week) - 1) * 7);
    const dow = simple.getDay();
    const ISOweekStart = simple;
    if (dow <= 4) {
      ISOweekStart.setDate(simple.getDate() - simple.getDay() + 1);
    } else {
      ISOweekStart.setDate(simple.getDate() + 8 - simple.getDay());
    }
    return ISOweekStart;
  }

  getWeekNumber(date: Date): number {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil((((d.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
  }

  formatWeekLabel(date: Date): string {
    const month = date.toLocaleString('default', { month: 'short' });
    const day = date.getDate();
    return `${month} ${day}`;
  }

  getHoursForWeek(dev: DeveloperWorkload, weekKey: string): number {
    return dev.weeklyLoad.get(weekKey) || 0;
  }

  getLoadPercentage(hours: number): number {
    return (hours / this.MAX_HOURS_PER_WEEK) * 100;
  }

  getHeatmapColor(hours: number): string {
    const percentage = this.getLoadPercentage(hours);
    
    if (hours === 0) {
      return '#f5f5f5'; // Empty
    } else if (percentage < 50) {
      return '#a8e6cf'; // Light green - underutilized
    } else if (percentage < 80) {
      return '#ffd93d'; // Yellow - moderate
    } else if (percentage <= 100) {
      return '#6bcf7f'; // Green - optimal
    } else if (percentage <= 125) {
      return '#ff9f43'; // Orange - overloaded
    } else {
      return '#ee5a6f'; // Red - severely overloaded
    }
  }

  getHeatmapClass(hours: number): string {
    const percentage = this.getLoadPercentage(hours);
    
    if (hours === 0) return 'empty';
    if (percentage < 50) return 'underutilized';
    if (percentage < 80) return 'moderate';
    if (percentage <= 100) return 'optimal';
    if (percentage <= 125) return 'overloaded';
    return 'critical';
  }

  isCurrentWeek(weekKey: string): boolean {
    const today = new Date();
    const currentWeekKey = this.getWeekKey(today);
    return weekKey === currentWeekKey;
  }
}
