import { Routes } from '@angular/router';
import { ProjectList } from './features/projects/project-list/project-list';

export const routes: Routes = [
  { path: '', redirectTo: '/projects', pathMatch: 'full' },
  { path: 'projects', component: ProjectList },
  { path: '**', redirectTo: '/projects' }
];
