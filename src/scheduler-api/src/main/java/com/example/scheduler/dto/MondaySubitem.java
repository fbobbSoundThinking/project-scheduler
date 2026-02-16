package com.example.scheduler.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class MondaySubitem {
    private String subitemId;
    private String subitemName;
    private String status;
    private List<String> developers = new ArrayList<>();
    private Timeline devTimeline;     // Developer Timeline (timeline_1__1)
    private Timeline qaTimeline;      // QA Timeline (timeline_14__1)
    private String targetDeploymentDate;  // Target Prod Date (date_mkmnnfpp)
    private Integer estimatedDays;    // Estimate - WD (numbers98)
    private Map<String, String> columns = new HashMap<>();
    
    @Data
    public static class Timeline {
        private String startDate;
        private String endDate;
    }
}
