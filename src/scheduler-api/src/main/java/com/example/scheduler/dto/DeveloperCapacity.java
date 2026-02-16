package com.example.scheduler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class DeveloperCapacity {
    private Integer developerId;
    private String developerName;
    private String position;
    private Map<LocalDate, BigDecimal> weeklyHours; // weekStart -> hours assigned
    
    public DeveloperCapacity() {}
    
    public DeveloperCapacity(Integer developerId, String developerName, 
                            String position, Map<LocalDate, BigDecimal> weeklyHours) {
        this.developerId = developerId;
        this.developerName = developerName;
        this.position = position;
        this.weeklyHours = weeklyHours;
    }
    
    public Integer getDeveloperId() {
        return developerId;
    }
    
    public void setDeveloperId(Integer developerId) {
        this.developerId = developerId;
    }
    
    public String getDeveloperName() {
        return developerName;
    }
    
    public void setDeveloperName(String developerName) {
        this.developerName = developerName;
    }
    
    public String getPosition() {
        return position;
    }
    
    public void setPosition(String position) {
        this.position = position;
    }
    
    public Map<LocalDate, BigDecimal> getWeeklyHours() {
        return weeklyHours;
    }
    
    public void setWeeklyHours(Map<LocalDate, BigDecimal> weeklyHours) {
        this.weeklyHours = weeklyHours;
    }
}
