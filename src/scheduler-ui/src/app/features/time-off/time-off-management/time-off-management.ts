import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { TimeOffService, DeveloperTimeOff } from '../../../core/services/time-off.service';
import { HolidayService, CompanyHoliday } from '../../../core/services/holiday.service';
import { DeveloperService } from '../../../core/services/developer.service';
import { Developer } from '../../../core/models';

@Component({
  selector: 'app-time-off-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './time-off-management.html',
  styleUrls: ['./time-off-management.scss']
})
export class TimeOffManagement implements OnInit {
  // Time Off
  timeOffList: DeveloperTimeOff[] = [];
  developers: Developer[] = [];
  
  newTimeOff: DeveloperTimeOff = {
    developer: { developersId: 0 },
    startDate: '',
    endDate: '',
    type: 'PTO',
    note: ''
  };
  
  editingTimeOff: DeveloperTimeOff | null = null;
  
  // Holidays
  holidaysList: CompanyHoliday[] = [];
  newHoliday: CompanyHoliday = {
    holidayDate: '',
    name: ''
  };
  
  editingHoliday: CompanyHoliday | null = null;
  
  // UI State
  loading: boolean = true;
  selectedYear: number = new Date().getFullYear();
  timeOffTypes = ['PTO', 'VACATION', 'SICK', 'OTHER'];

  constructor(
    private timeOffService: TimeOffService,
    private holidayService: HolidayService,
    private developerService: DeveloperService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    
    Promise.all([
      firstValueFrom(this.developerService.getAllDevelopers()),
      firstValueFrom(this.timeOffService.getAllTimeOff()),
      firstValueFrom(this.holidayService.getAllHolidays(this.selectedYear))
    ]).then(([developers, timeOff, holidays]) => {
      this.developers = developers || [];
      this.timeOffList = timeOff || [];
      this.holidaysList = holidays || [];
      this.loading = false;
      this.cdr.detectChanges();
    }).catch(error => {
      console.error('Error loading time-off data:', error);
      this.loading = false;
      this.cdr.detectChanges();
    });
  }

  // Time Off Methods
  addTimeOff(): void {
    if (!this.validateTimeOff(this.newTimeOff)) {
      alert('Please fill in all required fields');
      return;
    }

    this.timeOffService.createTimeOff(this.newTimeOff).subscribe({
      next: (timeOff) => {
        this.timeOffList.push(timeOff);
        this.resetNewTimeOff();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error creating time-off:', error);
        alert('Failed to create time-off entry');
      }
    });
  }

  editTimeOff(timeOff: DeveloperTimeOff): void {
    this.editingTimeOff = { ...timeOff };
  }

  saveTimeOff(): void {
    if (!this.editingTimeOff || !this.editingTimeOff.developerTimeOffId) {
      return;
    }

    this.timeOffService.updateTimeOff(this.editingTimeOff.developerTimeOffId, this.editingTimeOff).subscribe({
      next: (updated) => {
        const index = this.timeOffList.findIndex(t => t.developerTimeOffId === updated.developerTimeOffId);
        if (index >= 0) {
          this.timeOffList[index] = updated;
        }
        this.editingTimeOff = null;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error updating time-off:', error);
        alert('Failed to update time-off entry');
      }
    });
  }

  cancelEditTimeOff(): void {
    this.editingTimeOff = null;
  }

  deleteTimeOff(timeOff: DeveloperTimeOff): void {
    if (!timeOff.developerTimeOffId || !confirm('Are you sure you want to delete this time-off entry?')) {
      return;
    }

    this.timeOffService.deleteTimeOff(timeOff.developerTimeOffId).subscribe({
      next: () => {
        this.timeOffList = this.timeOffList.filter(t => t.developerTimeOffId !== timeOff.developerTimeOffId);
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error deleting time-off:', error);
        alert('Failed to delete time-off entry');
      }
    });
  }

  validateTimeOff(timeOff: DeveloperTimeOff): boolean {
    return timeOff.developer.developersId > 0 && 
           timeOff.startDate !== '' && 
           timeOff.endDate !== '' && 
           timeOff.type !== '';
  }

  resetNewTimeOff(): void {
    this.newTimeOff = {
      developer: { developersId: 0 },
      startDate: '',
      endDate: '',
      type: 'PTO',
      note: ''
    };
  }

  // Holiday Methods
  addHoliday(): void {
    if (!this.validateHoliday(this.newHoliday)) {
      alert('Please fill in all required fields');
      return;
    }

    this.holidayService.createHoliday(this.newHoliday).subscribe({
      next: (holiday) => {
        this.holidaysList.push(holiday);
        this.holidaysList.sort((a, b) => a.holidayDate.localeCompare(b.holidayDate));
        this.resetNewHoliday();
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error creating holiday:', error);
        alert('Failed to create holiday');
      }
    });
  }

  editHoliday(holiday: CompanyHoliday): void {
    this.editingHoliday = { ...holiday };
  }

  saveHoliday(): void {
    if (!this.editingHoliday || !this.editingHoliday.companyHolidayId) {
      return;
    }

    this.holidayService.updateHoliday(this.editingHoliday.companyHolidayId, this.editingHoliday).subscribe({
      next: (updated) => {
        const index = this.holidaysList.findIndex(h => h.companyHolidayId === updated.companyHolidayId);
        if (index >= 0) {
          this.holidaysList[index] = updated;
        }
        this.holidaysList.sort((a, b) => a.holidayDate.localeCompare(b.holidayDate));
        this.editingHoliday = null;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error updating holiday:', error);
        alert('Failed to update holiday');
      }
    });
  }

  cancelEditHoliday(): void {
    this.editingHoliday = null;
  }

  deleteHoliday(holiday: CompanyHoliday): void {
    if (!holiday.companyHolidayId || !confirm('Are you sure you want to delete this holiday?')) {
      return;
    }

    this.holidayService.deleteHoliday(holiday.companyHolidayId).subscribe({
      next: () => {
        this.holidaysList = this.holidaysList.filter(h => h.companyHolidayId !== holiday.companyHolidayId);
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error deleting holiday:', error);
        alert('Failed to delete holiday');
      }
    });
  }

  validateHoliday(holiday: CompanyHoliday): boolean {
    return holiday.holidayDate !== '' && holiday.name !== '';
  }

  resetNewHoliday(): void {
    this.newHoliday = {
      holidayDate: '',
      name: ''
    };
  }

  changeYear(year: number): void {
    this.selectedYear = year;
    this.holidayService.getAllHolidays(year).subscribe({
      next: (holidays) => {
        this.holidaysList = holidays;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading holidays:', error);
      }
    });
  }

  getDeveloperName(developersId: number): string {
    const dev = this.developers.find(d => d.developersId === developersId);
    return dev ? `${dev.firstName} ${dev.lastName}` : 'Unknown';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleDateString();
  }
}
