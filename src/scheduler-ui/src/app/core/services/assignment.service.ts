import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Assignment } from '../models';

@Injectable({
  providedIn: 'root'
})
export class AssignmentService {
  private apiUrl = 'http://localhost:8080/api/assignments';

  constructor(private http: HttpClient) {}

  getAllAssignments(): Observable<Assignment[]> {
    return this.http.get<Assignment[]>(this.apiUrl);
  }

  getAssignmentsByDeveloper(developerId: number): Observable<Assignment[]> {
    return this.http.get<Assignment[]>(`${this.apiUrl}/developer/${developerId}`);
  }

  getAssignmentsByProject(projectId: number): Observable<Assignment[]> {
    return this.http.get<Assignment[]>(`${this.apiUrl}/project/${projectId}`);
  }

  createAssignment(projectId: number, developerId: number, ratio: number = 1.0): Observable<Assignment> {
    const payload = { projectId, developerId, ratio };
    return this.http.post<Assignment>(this.apiUrl, payload);
  }

  createSubitemAssignment(subitemId: number, developerId: number, ratio: number = 1.0): Observable<Assignment> {
    const payload = { subitemId, developerId, ratio };
    return this.http.post<Assignment>(this.apiUrl, payload);
  }

  updateAssignment(id: number, assignment: Assignment): Observable<Assignment> {
    return this.http.put<Assignment>(`${this.apiUrl}/${id}`, assignment);
  }

  deleteAssignment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  checkConflict(developerId: number, startDate: string, endDate: string, ratio: number, excludeAssignmentId?: number): Observable<ConflictCheckResult> {
    let params = `developerId=${developerId}&startDate=${startDate}&endDate=${endDate}&ratio=${ratio}`;
    if (excludeAssignmentId) {
      params += `&excludeAssignmentId=${excludeAssignmentId}`;
    }
    return this.http.get<ConflictCheckResult>(`${this.apiUrl}/conflict-check?${params}`);
  }
}

export interface ConflictCheckResult {
  hasConflict: boolean;
  weeks: WeekConflict[];
}

export interface WeekConflict {
  weekStart: string;
  currentHours: number;
  newHours: number;
  totalHours: number;
  capacityHours: number;
  overageHours: number;
}
