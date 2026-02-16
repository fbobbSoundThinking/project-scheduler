import { Project } from './project.model';
import { Developer } from './developer.model';
import { Subitem } from './subitem.model';

export interface Assignment {
  assignmentsId?: number;
  project?: Project;
  subitem?: Subitem;
  developer?: Developer;
  startDate?: string;
  endDate?: string;
  ratio?: number;
}
