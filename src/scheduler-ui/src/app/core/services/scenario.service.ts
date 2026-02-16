import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TeamCapacityResponse, DeveloperCapacity } from './capacity.service';

export interface Scenario {
  scenarioId: number;
  name: string;
  description: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  status: 'DRAFT' | 'APPLIED' | 'ARCHIVED';
}

export interface ScenarioChange {
  scenarioAssignmentId: number;
  changeType: 'ADD' | 'MODIFY' | 'DELETE';
  originalAssignmentId: number | null;
  projectId: number | null;
  projectName?: string;
  developerId: number | null;
  developerName?: string;
  startDate: string | null;
  endDate: string | null;
  ratio: number | null;
}

export interface ScenarioDetail {
  scenario: Scenario;
  changes: ScenarioChange[];
}

@Injectable({
  providedIn: 'root'
})
export class ScenarioService {
  private apiUrl = 'http://localhost:8080/api/scenarios';

  constructor(private http: HttpClient) {}

  listScenarios(): Observable<Scenario[]> {
    return this.http.get<Scenario[]>(this.apiUrl);
  }

  createScenario(name: string, description: string, createdBy?: string): Observable<Scenario> {
    return this.http.post<Scenario>(this.apiUrl, {
      name,
      description: description || '',
      createdBy: createdBy || ''
    });
  }

  getScenario(scenarioId: number): Observable<Scenario> {
    return this.http.get<Scenario>(`${this.apiUrl}/${scenarioId}`);
  }

  deleteScenario(scenarioId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${scenarioId}`);
  }

  addChange(
    scenarioId: number,
    changeType: 'ADD' | 'MODIFY' | 'DELETE',
    originalAssignmentId: number | null,
    projectId: number | null,
    developerId: number | null,
    startDate: string | null,
    endDate: string | null,
    ratio: number | null
  ): Observable<ScenarioChange> {
    return this.http.post<ScenarioChange>(
      `${this.apiUrl}/${scenarioId}/changes`,
      {
        changeType,
        originalAssignmentId,
        projectId,
        developerId,
        startDate,
        endDate,
        ratio
      }
    );
  }

  removeChange(scenarioId: number, changeId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${scenarioId}/changes/${changeId}`);
  }

  applyScenario(scenarioId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${scenarioId}/apply`, {});
  }

  getCapacityWithScenario(
    scenarioId: number,
    startDate?: string,
    weeks?: number
  ): Observable<TeamCapacityResponse[]> {
    let params = new HttpParams();
    params = params.set('scenarioId', scenarioId.toString());
    if (startDate) {
      params = params.set('from', startDate);
    }
    if (weeks !== undefined && startDate) {
      const toDate = this.addWeeks(startDate, weeks);
      params = params.set('to', toDate);
    }
    return this.http.get<TeamCapacityResponse[]>('http://localhost:8080/api/capacity/teams', { params });
  }

  getTeamCapacityWithScenario(
    teamId: number,
    scenarioId: number,
    startDate?: string,
    weeks?: number
  ): Observable<TeamCapacityResponse> {
    let params = new HttpParams();
    params = params.set('scenarioId', scenarioId.toString());
    if (startDate) {
      params = params.set('from', startDate);
    }
    if (weeks !== undefined && startDate) {
      const toDate = this.addWeeks(startDate, weeks);
      params = params.set('to', toDate);
    }
    return this.http.get<TeamCapacityResponse>(
      `http://localhost:8080/api/capacity/team/${teamId}`,
      { params }
    );
  }

  getTeamDeveloperBreakdownWithScenario(
    teamId: number,
    scenarioId: number,
    startDate?: string,
    weeks?: number
  ): Observable<DeveloperCapacity[]> {
    let params = new HttpParams();
    params = params.set('scenarioId', scenarioId.toString());
    if (startDate) {
      params = params.set('from', startDate);
    }
    if (weeks !== undefined && startDate) {
      const toDate = this.addWeeks(startDate, weeks);
      params = params.set('to', toDate);
    }
    return this.http.get<DeveloperCapacity[]>(
      `http://localhost:8080/api/capacity/team/${teamId}/developers`,
      { params }
    );
  }

  private addWeeks(dateStr: string, weeks: number): string {
    const date = new Date(dateStr);
    date.setDate(date.getDate() + weeks * 7);
    return date.toISOString().split('T')[0];
  }
}
