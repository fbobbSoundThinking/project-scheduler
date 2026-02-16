import { Assignment } from './assignment.model';

export interface Developer {
  developersId?: number;
  firstName: string;
  lastName: string;
  teamsId?: number;
  position?: string;
  fullName?: string;
  assignments?: Assignment[];
}
