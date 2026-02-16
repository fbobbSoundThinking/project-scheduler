import { Routes } from '@angular/router';
import { ProjectList } from './features/projects/project-list/project-list';
import { GanttView } from './features/gantt/gantt-view/gantt-view';
import { WorkloadHeatmap } from './features/workload-heatmap/workload-heatmap';
import { CapacityDashboard } from './features/capacity/capacity-dashboard/capacity-dashboard';
import { ScenarioPlanner } from './features/scenarios/scenario-planner/scenario-planner';
import { TimeOffManagement } from './features/time-off/time-off-management/time-off-management';

export const routes: Routes = [
  { path: '', redirectTo: '/projects', pathMatch: 'full' },
  { path: 'projects', component: ProjectList },
  { path: 'gantt', component: GanttView },
  { path: 'workload', component: WorkloadHeatmap },
  { path: 'capacity', component: CapacityDashboard },
  { path: 'scenarios', component: ScenarioPlanner },
  { path: 'time-off', component: TimeOffManagement },
  { path: '**', redirectTo: '/projects' }
];
