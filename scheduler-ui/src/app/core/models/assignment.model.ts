import { Project } from './project.model';
import { Developer } from './developer.model';

export interface Assignment {
  assignmentsId?: number;
  project?: Project;
  developer?: Developer;
  startDate?: string;
  endDate?: string;
  ratio?: number;
}
