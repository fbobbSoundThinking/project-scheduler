package com.example.scheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "monday")
@Data
public class MondayConfig {
    private String apiKey;
    private String boardId;
    private List<String> groups;
    
    // Developer name mapping (Monday.com display name -> database full name)
    private Map<String, String> developerNameMap = new HashMap<>();
    
    public MondayConfig() {
        // Initialize developer name mappings
        initializeDeveloperNameMap();
    }
    
    private void initializeDeveloperNameMap() {
        developerNameMap.put("Arkopal Saha", "ARKOPAL SAHA");
        developerNameMap.put("Harishkumar Gundameedi", "HARISHKUMAR GUNDAMEEDI");
        developerNameMap.put("Harish Gundameedi", "HARISHKUMAR GUNDAMEEDI");
        developerNameMap.put("Satish Komireddy", "SATISH KOMIREDDY");
        developerNameMap.put("Venugopal Korukonda", "VENUGOPAL KORUKONDA");
        developerNameMap.put("Mahesh Bollineni", "MAHESH BOLLINENI");
        developerNameMap.put("Luke Nolan", "LUKE NOLAN");
        developerNameMap.put("Frank Bobb", "FRANK BOBB");
        developerNameMap.put("Saurabh Mishra", "SAURABH MISHRA");
        developerNameMap.put("Ram Kotapati", "RAM KOTAPATI");
        developerNameMap.put("Rick Martin", "RICK MARTIN");
        developerNameMap.put("Tom Ross", "TOM ROSS");
    }
    
    public String getNormalizedDeveloperName(String mondayName) {
        return developerNameMap.getOrDefault(mondayName, mondayName.toUpperCase());
    }
}
