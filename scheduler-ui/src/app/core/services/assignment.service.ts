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

  createAssignment(assignment: Assignment): Observable<Assignment> {
    return this.http.post<Assignment>(this.apiUrl, assignment);
  }

  updateAssignment(id: number, assignment: Assignment): Observable<Assignment> {
    return this.http.put<Assignment>(`${this.apiUrl}/${id}`, assignment);
  }

  deleteAssignment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
