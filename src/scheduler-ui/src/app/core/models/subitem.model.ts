import { Project } from './project.model';
import { Assignment } from './assignment.model';

export interface Subitem {
  subitemsId?: number;
  subitemId: string;
  subitemName: string;
  status?: string;
  group?: string;
  estimatedDays?: number;
  devStartDate?: string;
  devEndDate?: string;
  qaStartDate?: string;
  qaEndDate?: string;
  targetDeploymentDate?: string;
  project?: Project;
  assignments?: Assignment[];
}
