import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Subitem } from '../models';

@Injectable({
  providedIn: 'root'
})
export class SubitemService {
  private apiUrl = 'http://localhost:8080/api/subitems';

  constructor(private http: HttpClient) {}

  getAllSubitems(projectId?: number): Observable<Subitem[]> {
    if (projectId) {
      return this.http.get<Subitem[]>(this.apiUrl, {
        params: { projectId: projectId.toString() }
      });
    }
    return this.http.get<Subitem[]>(this.apiUrl);
  }

  getSubitemById(id: number): Observable<Subitem> {
    return this.http.get<Subitem>(`${this.apiUrl}/${id}`);
  }

  getSubitemsByStatus(status: string): Observable<Subitem[]> {
    return this.http.get<Subitem[]>(`${this.apiUrl}/status/${status}`);
  }

  createSubitem(subitem: Subitem): Observable<Subitem> {
    return this.http.post<Subitem>(this.apiUrl, subitem);
  }

  updateSubitem(id: number, subitem: Subitem): Observable<Subitem> {
    return this.http.put<Subitem>(`${this.apiUrl}/${id}`, subitem);
  }

  deleteSubitem(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
