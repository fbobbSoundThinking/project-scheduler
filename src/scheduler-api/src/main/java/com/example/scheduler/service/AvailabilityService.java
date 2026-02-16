package com.example.scheduler.service;

import com.example.scheduler.model.CompanyHoliday;
import com.example.scheduler.model.DeveloperTimeOff;
import com.example.scheduler.repository.CompanyHolidayRepository;
import com.example.scheduler.repository.DeveloperTimeOffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class AvailabilityService {
    
    private static final int MAX_HOURS_PER_WEEK = 32;
    private static final int HOURS_PER_DAY = 8;
    
    @Autowired
    private DeveloperTimeOffRepository timeOffRepository;
    
    @Autowired
    private CompanyHolidayRepository holidayRepository;
    
    /**
     * Get the number of business days a developer is off during a specific week.
     * Business days are Monday-Friday only. Weekends are excluded.
     * Company holidays that overlap with developer time-off are deduplicated (counted only once).
     */
    public int getBusinessDaysOff(Integer developerId, LocalDate weekStart, LocalDate weekEnd) {
        if (developerId == null || weekStart == null || weekEnd == null) {
            return 0;
        }
        
        // Get all time-off entries for this developer that overlap with the week
        List<DeveloperTimeOff> timeOffList = timeOffRepository.findByDeveloperIdAndDateRange(
            developerId, weekStart, weekEnd
        );
        
        // Get all company holidays that fall within the week
        List<CompanyHoliday> holidays = holidayRepository.findByDateRange(weekStart, weekEnd);
        
        // Build a set of all dates the developer is off (business days only)
        Set<LocalDate> daysOff = new HashSet<>();
        
        // Add developer time-off dates
        for (DeveloperTimeOff timeOff : timeOffList) {
            LocalDate current = timeOff.getStartDate();
            LocalDate end = timeOff.getEndDate();
            
            // Ensure we stay within the week boundaries
            if (current.isBefore(weekStart)) {
                current = weekStart;
            }
            if (end.isAfter(weekEnd)) {
                end = weekEnd;
            }
            
            while (!current.isAfter(end)) {
                if (isBusinessDay(current)) {
                    daysOff.add(current);
                }
                current = current.plusDays(1);
            }
        }
        
        // Add company holidays (automatically deduplicated by Set)
        for (CompanyHoliday holiday : holidays) {
            LocalDate holidayDate = holiday.getHolidayDate();
            if (!holidayDate.isBefore(weekStart) && !holidayDate.isAfter(weekEnd) && isBusinessDay(holidayDate)) {
                daysOff.add(holidayDate);
            }
        }
        
        return daysOff.size();
    }
    
    /**
     * Get the adjusted capacity for a developer for a specific week.
     * Adjusted capacity = MAX_HOURS_PER_WEEK - (businessDaysOff * HOURS_PER_DAY)
     * Result is clamped to a minimum of 0.
     */
    public BigDecimal getAdjustedCapacity(Integer developerId, LocalDate weekStart) {
        if (developerId == null || weekStart == null) {
            return new BigDecimal(MAX_HOURS_PER_WEEK);
        }
        
        LocalDate weekEnd = weekStart.plusDays(6);
        int daysOff = getBusinessDaysOff(developerId, weekStart, weekEnd);
        
        int adjustedHours = MAX_HOURS_PER_WEEK - (daysOff * HOURS_PER_DAY);
        
        // Clamp to 0 minimum
        if (adjustedHours < 0) {
            adjustedHours = 0;
        }
        
        return new BigDecimal(adjustedHours);
    }
    
    /**
     * Get the total adjusted capacity for all developers in a team for a specific week.
     */
    public BigDecimal getTeamAdjustedCapacity(List<Integer> developerIds, LocalDate weekStart) {
        if (developerIds == null || developerIds.isEmpty() || weekStart == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalCapacity = BigDecimal.ZERO;
        for (Integer developerId : developerIds) {
            totalCapacity = totalCapacity.add(getAdjustedCapacity(developerId, weekStart));
        }
        
        return totalCapacity;
    }
    
    /**
     * Check if a date is a business day (Monday-Friday).
     */
    private boolean isBusinessDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }
}
