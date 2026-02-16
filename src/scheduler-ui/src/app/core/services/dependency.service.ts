import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DependencyService {
  private apiUrl = 'http://localhost:8080/api/dependencies';

  constructor(private http: HttpClient) {}

  getDependencies(projectId: number): Observable<ProjectDependency[]> {
    return this.http.get<ProjectDependency[]>(`${this.apiUrl}/project/${projectId}`);
  }

  createDependency(predecessorId: number, successorId: number, dependencyType: string = 'FINISH_TO_START'): Observable<ProjectDependency> {
    const payload = { predecessorId, successorId, dependencyType };
    return this.http.post<ProjectDependency>(this.apiUrl, payload);
  }

  deleteDependency(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

export interface ProjectDependency {
  projectDependenciesId: number;
  predecessor: {
    projectsId: number;
    projectName: string;
  };
  successor: {
    projectsId: number;
    projectName: string;
  };
  dependencyType: string;
}
