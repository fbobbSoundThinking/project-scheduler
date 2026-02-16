import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DeveloperTimeOff {
  developerTimeOffId?: number;
  developer: {
    developersId: number;
    firstName?: string;
    lastName?: string;
  };
  startDate: string;
  endDate: string;
  type: string;
  note?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TimeOffService {
  private apiUrl = 'http://localhost:8080/api/time-off';

  constructor(private http: HttpClient) {}

  getAllTimeOff(from?: string, to?: string): Observable<DeveloperTimeOff[]> {
    let params = new HttpParams();
    if (from) {
      params = params.set('from', from);
    }
    if (to) {
      params = params.set('to', to);
    }
    return this.http.get<DeveloperTimeOff[]>(this.apiUrl, { params });
  }

  getTimeOffByDeveloper(developerId: number): Observable<DeveloperTimeOff[]> {
    return this.http.get<DeveloperTimeOff[]>(`${this.apiUrl}/developer/${developerId}`);
  }

  getTimeOffById(id: number): Observable<DeveloperTimeOff> {
    return this.http.get<DeveloperTimeOff>(`${this.apiUrl}/${id}`);
  }

  createTimeOff(timeOff: DeveloperTimeOff): Observable<DeveloperTimeOff> {
    return this.http.post<DeveloperTimeOff>(this.apiUrl, timeOff);
  }

  updateTimeOff(id: number, timeOff: DeveloperTimeOff): Observable<DeveloperTimeOff> {
    return this.http.put<DeveloperTimeOff>(`${this.apiUrl}/${id}`, timeOff);
  }

  deleteTimeOff(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
