package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "company_holidays")
@Data
public class CompanyHoliday {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_holiday_id")
    private Integer companyHolidayId;
    
    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;
    
    @Column(name = "name", length = 100, nullable = false)
    private String name;
}
