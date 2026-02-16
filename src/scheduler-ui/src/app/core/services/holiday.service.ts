import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CompanyHoliday {
  companyHolidayId?: number;
  holidayDate: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class HolidayService {
  private apiUrl = 'http://localhost:8080/api/holidays';

  constructor(private http: HttpClient) {}

  getAllHolidays(year?: number): Observable<CompanyHoliday[]> {
    let params = new HttpParams();
    if (year) {
      params = params.set('year', year.toString());
    }
    return this.http.get<CompanyHoliday[]>(this.apiUrl, { params });
  }

  getHolidayById(id: number): Observable<CompanyHoliday> {
    return this.http.get<CompanyHoliday>(`${this.apiUrl}/${id}`);
  }

  createHoliday(holiday: CompanyHoliday): Observable<CompanyHoliday> {
    return this.http.post<CompanyHoliday>(this.apiUrl, holiday);
  }

  updateHoliday(id: number, holiday: CompanyHoliday): Observable<CompanyHoliday> {
    return this.http.put<CompanyHoliday>(`${this.apiUrl}/${id}`, holiday);
  }

  deleteHoliday(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
