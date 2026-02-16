import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ScenarioService, Scenario, ScenarioChange } from '../../../core/services/scenario.service';
import { ProjectService } from '../../../core/services/project.service';
import { DeveloperService } from '../../../core/services/developer.service';
import { CapacityService, TeamCapacityResponse, WeekCapacity } from '../../../core/services/capacity.service';

interface LocalProject {
  projectsId: number;
  projectName: string;
}

interface LocalDeveloper {
  developersId: number;
  fullName: string;
}

interface NewChange {
  changeType: 'ADD' | 'MODIFY' | 'DELETE';
  originalAssignmentId?: number;
  projectId?: number;
  developerId?: number;
  startDate?: string;
  endDate?: string;
  ratio?: number;
}

interface WeekDelta {
  weekStart: string;
  baselineHours: number;
  scenarioHours: number;
  delta: number;
  baselineUtilization: number;
  scenarioUtilization: number;
}

interface TeamComparison {
  teamId: number;
  teamName: string;
  baselineWeeks: WeekCapacity[];
  scenarioWeeks: WeekCapacity[];
  weekDeltas: WeekDelta[];
  totalDeltaHours: number;
}

@Component({
  selector: 'app-scenario-planner',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './scenario-planner.html',
  styleUrls: ['./scenario-planner.scss']
})
export class ScenarioPlanner implements OnInit {
  scenarios: Scenario[] = [];
  activeScenario: Scenario | null = null;
  changes: ScenarioChange[] = [];
  projects: LocalProject[] = [];
  developers: LocalDeveloper[] = [];
  scenarioCapacity: TeamCapacityResponse[] = [];
  baselineCapacity: TeamCapacityResponse[] = [];
  teamComparisons: TeamComparison[] = [];
  summaryStats = {
    totalHoursAdded: 0,
    totalHoursRemoved: 0,
    netChange: 0
  };

  // UI state
  isCreatingScenario = false;
  isAddingChange = false;
  newScenarioName = '';
  newScenarioDescription = '';
  newChange: NewChange = { changeType: 'ADD' };
  showDeleteConfirm = false;
  deleteConfirmId = 0;
  dateRange = { startDate: '', weeks: 12 };

  constructor(
    private scenarioService: ScenarioService,
    private projectService: ProjectService,
    private developerService: DeveloperService,
    private capacityService: CapacityService
  ) {}

  ngOnInit(): void {
    this.loadScenarios();
    this.loadProjects();
    this.loadDevelopers();
    const today = new Date().toISOString().split('T')[0];
    this.dateRange.startDate = today;
  }

  loadScenarios(): void {
    this.scenarioService.listScenarios().subscribe((scenarios) => {
      this.scenarios = scenarios.filter(s => s.status === 'DRAFT');
    });
  }

  loadProjects(): void {
    this.projectService.getAllProjects().subscribe((projects: any[]) => {
      this.projects = projects.map(p => ({
        projectsId: p.projectsId || p.id,
        projectName: p.projectName || p.name
      }));
    });
  }

  loadDevelopers(): void {
    this.developerService.getAllDevelopers().subscribe((developers: any[]) => {
      this.developers = developers.map(d => ({
        developersId: d.developersId || d.id,
        fullName: d.fullName || d.name
      }));
    });
  }

  selectScenario(scenario: Scenario | null): void {
    this.activeScenario = scenario;
    if (scenario) {
      this.loadChanges();
      this.loadCapacityData();
    }
  }

  loadChanges(): void {
    if (!this.activeScenario) return;
    this.changes = [];
  }

  loadCapacityData(): void {
    if (!this.activeScenario || !this.dateRange.startDate) return;

    // Load baseline and scenario capacity in parallel
    this.capacityService.getAllTeamsCapacity(this.dateRange.startDate, this.dateRange.weeks).subscribe(
      (baseline) => {
        this.baselineCapacity = baseline;
        this.calculateComparison();
      }
    );

    this.scenarioService.getCapacityWithScenario(this.activeScenario.scenarioId, this.dateRange.startDate, this.dateRange.weeks).subscribe(
      (scenario) => {
        this.scenarioCapacity = scenario;
        this.calculateComparison();
      }
    );
  }

  calculateComparison(): void {
    if (this.baselineCapacity.length === 0 || this.scenarioCapacity.length === 0) return;

    this.teamComparisons = [];
    this.summaryStats = { totalHoursAdded: 0, totalHoursRemoved: 0, netChange: 0 };

    for (const scenarioTeam of this.scenarioCapacity) {
      const baselineTeam = this.baselineCapacity.find(t => t.teamId === scenarioTeam.teamId);
      if (!baselineTeam) continue;

      const weekDeltas: WeekDelta[] = [];
      let teamTotalDelta = 0;

      for (let i = 0; i < scenarioTeam.weeks.length; i++) {
        const baselineWeek = baselineTeam.weeks[i];
        const scenarioWeek = scenarioTeam.weeks[i];

        const delta = scenarioWeek.assignedHours - baselineWeek.assignedHours;
        teamTotalDelta += delta;

        weekDeltas.push({
          weekStart: scenarioWeek.weekStart,
          baselineHours: baselineWeek.assignedHours,
          scenarioHours: scenarioWeek.assignedHours,
          delta,
          baselineUtilization: baselineWeek.utilization,
          scenarioUtilization: scenarioWeek.utilization
        });

        // Update summary
        if (delta > 0) {
          this.summaryStats.totalHoursAdded += delta;
        } else if (delta < 0) {
          this.summaryStats.totalHoursRemoved += Math.abs(delta);
        }
      }

      this.teamComparisons.push({
        teamId: scenarioTeam.teamId,
        teamName: scenarioTeam.teamName,
        baselineWeeks: baselineTeam.weeks,
        scenarioWeeks: scenarioTeam.weeks,
        weekDeltas,
        totalDeltaHours: teamTotalDelta
      });
    }

    this.summaryStats.netChange = this.summaryStats.totalHoursAdded - this.summaryStats.totalHoursRemoved;
  }

  createScenario(): void {
    if (!this.newScenarioName.trim()) return;

    this.scenarioService
      .createScenario(this.newScenarioName, this.newScenarioDescription)
      .subscribe((scenario) => {
        this.activeScenario = scenario;
        this.newScenarioName = '';
        this.newScenarioDescription = '';
        this.isCreatingScenario = false;
        this.loadScenarios();
        this.changes = [];
      });
  }

  deleteScenario(id: number): void {
    this.scenarioService.deleteScenario(id).subscribe(() => {
      this.loadScenarios();
      if (this.activeScenario?.scenarioId === id) {
        this.activeScenario = null;
        this.changes = [];
      }
      this.showDeleteConfirm = false;
    });
  }

  addChange(): void {
    if (!this.activeScenario) return;
    if (!this.newChange.changeType) return;
    if (
      this.newChange.changeType === 'ADD' &&
      (!this.newChange.projectId || !this.newChange.developerId)
    ) {
      return;
    }

    this.scenarioService
      .addChange(
        this.activeScenario.scenarioId,
        this.newChange.changeType,
        this.newChange.originalAssignmentId || null,
        this.newChange.projectId || null,
        this.newChange.developerId || null,
        this.newChange.startDate || null,
        this.newChange.endDate || null,
        this.newChange.ratio || null
      )
      .subscribe((change) => {
        this.changes.push(change);
        this.newChange = { changeType: 'ADD' };
        this.isAddingChange = false;
        this.loadCapacityData();
      });
  }

  removeChange(changeId: number): void {
    if (!this.activeScenario) return;
    this.scenarioService.removeChange(this.activeScenario.scenarioId, changeId).subscribe(() => {
      this.changes = this.changes.filter(c => c.scenarioAssignmentId !== changeId);
      this.loadCapacityData();
    });
  }

  applyScenario(): void {
    if (!this.activeScenario) return;
    if (!confirm('Are you sure you want to apply this scenario? This will modify live assignments.')) {
      return;
    }

    this.scenarioService.applyScenario(this.activeScenario.scenarioId).subscribe(() => {
      this.loadScenarios();
      this.activeScenario = null;
      this.changes = [];
      alert('Scenario applied successfully!');
    });
  }

  getProjectName(projectId: number | null): string {
    if (!projectId) return '';
    return this.projects.find(p => p.projectsId === projectId)?.projectName || `Project #${projectId}`;
  }

  getDeveloperName(developerId: number | null): string {
    if (!developerId) return '';
    return this.developers.find(d => d.developersId === developerId)?.fullName || `Developer #${developerId}`;
  }

  getDeltaClass(delta: number): string {
    if (delta > 0) return 'delta-positive';
    if (delta < 0) return 'delta-negative';
    return 'delta-neutral';
  }

  getDeltaIcon(delta: number): string {
    if (delta > 0) return '↑';
    if (delta < 0) return '↓';
    return '→';
  }

  cancelCreate(): void {
    this.isCreatingScenario = false;
    this.newScenarioName = '';
    this.newScenarioDescription = '';
  }

  cancelAddChange(): void {
    this.isAddingChange = false;
    this.newChange = { changeType: 'ADD' };
  }

  showDeleteConfirmDialog(id: number): void {
    this.showDeleteConfirm = true;
    this.deleteConfirmId = id;
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.deleteConfirmId = 0;
  }
}
