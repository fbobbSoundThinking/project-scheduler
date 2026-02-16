import { Assignment } from './assignment.model';
import { Subitem } from './subitem.model';

export interface Project {
  projectsId?: number;
  projectName: string;
  status?: string;
  group?: string;
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
  subitems?: Subitem[];
}
