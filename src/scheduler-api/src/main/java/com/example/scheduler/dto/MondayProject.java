package com.example.scheduler.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Data
public class MondayProject {
    private String itemId;
    private String projectName;
    private String status;
    private String group;
    private String developerTeam;
    private List<String> developers = new ArrayList<>();
    private Timeline timeline;
    private String prjNumber;
    private Map<String, String> columns = new HashMap<>();
    private List<MondaySubitem> subitems = new ArrayList<>();
    
    @Data
    public static class Timeline {
        private String startDate;
        private String endDate;
    }
}
