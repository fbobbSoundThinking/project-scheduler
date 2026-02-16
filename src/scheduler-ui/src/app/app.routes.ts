import { Routes } from '@angular/router';
import { ProjectList } from './features/projects/project-list/project-list';
import { GanttView } from './features/gantt/gantt-view/gantt-view';
import { WorkloadHeatmap } from './features/workload-heatmap/workload-heatmap';

export const routes: Routes = [
  { path: '', redirectTo: '/projects', pathMatch: 'full' },
  { path: 'projects', component: ProjectList },
  { path: 'gantt', component: GanttView },
  { path: 'workload', component: WorkloadHeatmap },
  { path: '**', redirectTo: '/projects' }
];
