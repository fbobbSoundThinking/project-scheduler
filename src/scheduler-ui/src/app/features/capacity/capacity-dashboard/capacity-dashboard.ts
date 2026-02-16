import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CapacityService, TeamCapacityResponse, WeekCapacity, DeveloperCapacity } from '../../../core/services/capacity.service';

interface TeamDevelopers {
  developers: DeveloperCapacity[];
  weekStarts: string[];
}

interface ExpandedTeamState {
  [teamId: number]: boolean;
}

@Component({
  selector: 'app-capacity-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './capacity-dashboard.html',
  styleUrls: ['./capacity-dashboard.scss']
})
export class CapacityDashboard implements OnInit {
  teams: TeamCapacityResponse[] = [];
  developerData: Map<number, TeamDevelopers> = new Map();
  expandedTeams: ExpandedTeamState = {};
  loading: boolean = true;
  
  // Controls
  startDate: string;
  numWeeks: number = 12;
  teamFilters: Map<number, boolean> = new Map();
  viewMode: 'hours' | 'utilization' = 'hours';
  
  // Constants
  readonly MAX_HOURS_PER_WEEK = 32;

  constructor(
    private capacityService: CapacityService,
    private cdr: ChangeDetectorRef
  ) {
    // Default to today
    const today = new Date();
    this.startDate = this.formatDate(today);
  }

  ngOnInit(): void {
    this.loadCapacityData();
  }

  loadCapacityData(): void {
    this.loading = true;
    
    this.capacityService.getAllTeamsCapacity(this.startDate, this.numWeeks).subscribe({
      next: (teams) => {
        this.teams = teams;
        // Initialize team filters (all enabled by default)
        teams.forEach(team => {
          this.teamFilters.set(team.teamId, true);
        });
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading capacity data:', err);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  getFilteredTeams(): TeamCapacityResponse[] {
    return this.teams.filter(team => this.teamFilters.get(team.teamId) !== false);
  }

  toggleTeam(teamId: number): void {
    if (this.expandedTeams[teamId]) {
      // Collapse
      this.expandedTeams[teamId] = false;
    } else {
      // Expand - load developer breakdown if not already loaded
      this.expandedTeams[teamId] = true;
      if (!this.developerData.has(teamId)) {
        this.loadDeveloperBreakdown(teamId);
      }
    }
  }

  loadDeveloperBreakdown(teamId: number): void {
    this.capacityService.getTeamDeveloperBreakdown(teamId, this.startDate, this.numWeeks).subscribe({
      next: (developers) => {
        // Get week starts from the team data
        const team = this.teams.find(t => t.teamId === teamId);
        const weekStarts = team ? team.weeks.map(w => w.weekStart) : [];
        
        this.developerData.set(teamId, {
          developers: developers,
          weekStarts: weekStarts
        });
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(`Error loading developer breakdown for team ${teamId}:`, err);
      }
    });
  }

  onDateRangeChange(): void {
    this.developerData.clear();
    this.expandedTeams = {};
    this.loadCapacityData();
  }

  toggleTeamFilter(teamId: number): void {
    const current = this.teamFilters.get(teamId);
    this.teamFilters.set(teamId, !current);
  }

  getTeamAverageUtilization(team: TeamCapacityResponse): number {
    if (team.weeks.length === 0) return 0;
    const totalUtil = team.weeks.reduce((sum, week) => sum + week.utilization, 0);
    return totalUtil / team.weeks.length;
  }

  getUtilizationColor(utilization: number): string {
    if (utilization < 70) return 'low';
    if (utilization < 90) return 'medium';
    return 'high';
  }

  getAvailabilityColor(week: WeekCapacity): string {
    // Inverted: green = high availability, red = low availability
    if (week.availableHours > week.totalCapacity * 0.3) return 'high-avail';
    if (week.availableHours > week.totalCapacity * 0.1) return 'medium-avail';
    return 'low-avail';
  }

  isCurrentWeek(weekStart: string): boolean {
    const today = new Date();
    const mondayOfThisWeek = this.getMondayOfWeek(today);
    return weekStart === this.formatDate(mondayOfThisWeek);
  }

  formatWeekLabel(weekStart: string): string {
    const date = new Date(weekStart);
    return `${date.getMonth() + 1}/${date.getDate()}`;
  }

  getDeveloperHours(developer: DeveloperCapacity, weekStart: string): number {
    return developer.weeklyHours[weekStart] || 0;
  }

  getDeveloperUtilization(developer: DeveloperCapacity, weekStart: string): number {
    const hours = this.getDeveloperHours(developer, weekStart);
    return (hours / this.MAX_HOURS_PER_WEEK) * 100;
  }

  getDeveloperCellColor(developer: DeveloperCapacity, weekStart: string): string {
    const utilization = this.getDeveloperUtilization(developer, weekStart);
    if (utilization < 70) return 'low';
    if (utilization < 90) return 'medium';
    return 'high';
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private getMondayOfWeek(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust when day is Sunday
    return new Date(d.setDate(diff));
  }
}
