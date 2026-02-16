package com.example.scheduler.dto;

import java.math.BigDecimal;
import java.util.List;

public class TeamCapacityResponse {
    private Integer teamId;
    private String teamName;
    private List<WeekCapacity> weeks;
    private BigDecimal averageUtilization;
    private int developerCount;
    
    public TeamCapacityResponse() {}
    
    public TeamCapacityResponse(Integer teamId, String teamName, 
                               List<WeekCapacity> weeks,
                               BigDecimal averageUtilization, 
                               int developerCount) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.weeks = weeks;
        this.averageUtilization = averageUtilization;
        this.developerCount = developerCount;
    }
    
    public Integer getTeamId() {
        return teamId;
    }
    
    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }
    
    public String getTeamName() {
        return teamName;
    }
    
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    
    public List<WeekCapacity> getWeeks() {
        return weeks;
    }
    
    public void setWeeks(List<WeekCapacity> weeks) {
        this.weeks = weeks;
    }
    
    public BigDecimal getAverageUtilization() {
        return averageUtilization;
    }
    
    public void setAverageUtilization(BigDecimal averageUtilization) {
        this.averageUtilization = averageUtilization;
    }
    
    public int getDeveloperCount() {
        return developerCount;
    }
    
    public void setDeveloperCount(int developerCount) {
        this.developerCount = developerCount;
    }
}
