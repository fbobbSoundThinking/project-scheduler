import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WeekCapacity {
  weekStart: string;
  totalCapacity: number;
  assignedHours: number;
  availableHours: number;
  utilization: number;
}

export interface TeamCapacityResponse {
  teamId: number;
  teamName: string;
  developerCount: number;
  weeks: WeekCapacity[];
}

export interface DeveloperCapacity {
  developerId: number;
  developerName: string;
  position: string;
  weeklyHours: { [weekStart: string]: number };
}

@Injectable({
  providedIn: 'root'
})
export class CapacityService {
  private apiUrl = 'http://localhost:8080/api/capacity';

  constructor(private http: HttpClient) {}

  getAllTeamsCapacity(startDate?: string, weeks?: number, scenarioId?: number): Observable<TeamCapacityResponse[]> {
    let params = new HttpParams();
    if (scenarioId) {
      params = params.set('scenarioId', scenarioId.toString());
    }
    if (startDate) {
      params = params.set('from', startDate);
    }
    if (weeks !== undefined && startDate) {
      const toDate = this.addWeeks(startDate, weeks);
      params = params.set('to', toDate);
    }
    return this.http.get<TeamCapacityResponse[]>(`${this.apiUrl}/teams`, { params });
  }

  getTeamCapacity(teamId: number, startDate?: string, weeks?: number, scenarioId?: number): Observable<TeamCapacityResponse> {
    let params = new HttpParams();
    if (scenarioId) {
      params = params.set('scenarioId', scenarioId.toString());
    }
    if (startDate) {
      params = params.set('from', startDate);
    }
    if (weeks !== undefined && startDate) {
      const toDate = this.addWeeks(startDate, weeks);
      params = params.set('to', toDate);
    }
    return this.http.get<TeamCapacityResponse>(`${this.apiUrl}/team/${teamId}`, { params });
  }

  getTeamDeveloperBreakdown(teamId: number, startDate?: string, weeks?: number, scenarioId?: number): Observable<DeveloperCapacity[]> {
    let params = new HttpParams();
    if (scenarioId) {
      params = params.set('scenarioId', scenarioId.toString());
    }
    if (startDate) {
      params = params.set('from', startDate);
    }
    if (weeks !== undefined && startDate) {
      const toDate = this.addWeeks(startDate, weeks);
      params = params.set('to', toDate);
    }
    return this.http.get<DeveloperCapacity[]>(`${this.apiUrl}/team/${teamId}/developers`, { params });
  }

  private addWeeks(dateStr: string, weeks: number): string {
    const date = new Date(dateStr);
    date.setDate(date.getDate() + (weeks * 7));
    return date.toISOString().split('T')[0];
  }
}
