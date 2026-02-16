package com.example.scheduler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WeekCapacity {
    private LocalDate weekStart;
    private BigDecimal totalCapacityHours;
    private BigDecimal assignedHours;
    private BigDecimal availableHours;
    private BigDecimal utilizationPercent;
    private int developerCount;
    
    public WeekCapacity() {}
    
    public WeekCapacity(LocalDate weekStart, BigDecimal totalCapacityHours, 
                       BigDecimal assignedHours, BigDecimal availableHours,
                       BigDecimal utilizationPercent, int developerCount) {
        this.weekStart = weekStart;
        this.totalCapacityHours = totalCapacityHours;
        this.assignedHours = assignedHours;
        this.availableHours = availableHours;
        this.utilizationPercent = utilizationPercent;
        this.developerCount = developerCount;
    }
    
    public LocalDate getWeekStart() {
        return weekStart;
    }
    
    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }
    
    public BigDecimal getTotalCapacityHours() {
        return totalCapacityHours;
    }
    
    public void setTotalCapacityHours(BigDecimal totalCapacityHours) {
        this.totalCapacityHours = totalCapacityHours;
    }
    
    public BigDecimal getAssignedHours() {
        return assignedHours;
    }
    
    public void setAssignedHours(BigDecimal assignedHours) {
        this.assignedHours = assignedHours;
    }
    
    public BigDecimal getAvailableHours() {
        return availableHours;
    }
    
    public void setAvailableHours(BigDecimal availableHours) {
        this.availableHours = availableHours;
    }
    
    public BigDecimal getUtilizationPercent() {
        return utilizationPercent;
    }
    
    public void setUtilizationPercent(BigDecimal utilizationPercent) {
        this.utilizationPercent = utilizationPercent;
    }
    
    public int getDeveloperCount() {
        return developerCount;
    }
    
    public void setDeveloperCount(int developerCount) {
        this.developerCount = developerCount;
    }
}
