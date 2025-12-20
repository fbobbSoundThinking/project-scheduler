import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Developer } from '../models';

@Injectable({
  providedIn: 'root'
})
export class DeveloperService {
  private apiUrl = 'http://localhost:8080/api/developers';

  constructor(private http: HttpClient) {}

  getAllDevelopers(): Observable<Developer[]> {
    return this.http.get<Developer[]>(this.apiUrl);
  }

  getDeveloperById(id: number): Observable<Developer> {
    return this.http.get<Developer>(`${this.apiUrl}/${id}`);
  }

  getDevelopersByTeam(teamId: number): Observable<Developer[]> {
    return this.http.get<Developer[]>(`${this.apiUrl}/team/${teamId}`);
  }
}
