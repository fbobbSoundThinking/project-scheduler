import { Assignment } from './assignment.model';

export interface Project {
  projectsId?: number;
  projectName: string;
  status?: string;
  level?: string;
  targetProdDate?: string;
  primaryTeamId?: number;
  webFocus?: number;
  priority?: number;
  urgency?: number;
  devHours?: number;
  wfHours?: number;
  qaHours?: number;
  primaryAppId?: number;
  primaryAppName?: string;
  startDate?: string;
  qaReadyDate?: string;
  itbNumber?: string;
  prjNumber?: string;
  assignments?: Assignment[];
}
