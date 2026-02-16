package com.example.scheduler.service;

import com.example.scheduler.config.MondayConfig;
import com.example.scheduler.dto.MondayProject;
import com.example.scheduler.dto.MondaySubitem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MondayService {
    
    private static final String MONDAY_API_URL = "https://api.monday.com/v2";
    
    @Autowired
    private MondayConfig mondayConfig;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public List<MondayProject> fetchProjectsFromMonday() {
        List<MondayProject> projects = new ArrayList<>();
        
        try {
            String cursor = null;
            int pageNum = 1;
            
            while (true) {
                log.info("Fetching page {} from Monday.com", pageNum);
                
                String query = buildGraphQLQuery(cursor);
                JsonNode response = executeGraphQLQuery(query);
                
                if (response == null) {
                    break;
                }
                
                JsonNode boardsNode = response.path("data").path("boards");
                if (!boardsNode.isArray() || boardsNode.size() == 0) {
                    break;
                }
                
                JsonNode board = boardsNode.get(0);
                JsonNode itemsPage = board.path("items_page");
                JsonNode items = itemsPage.path("items");
                
                if (!items.isArray() || items.size() == 0) {
                    break;
                }
                
                // Process items
                for (JsonNode item : items) {
                    MondayProject project = parseProjectItem(item);
                    if (project != null) {
                        projects.add(project);
                    }
                }
                
                // Check for next page
                cursor = itemsPage.path("cursor").asText(null);
                if (cursor == null || cursor.isEmpty()) {
                    break;
                }
                
                pageNum++;
            }
            
            log.info("Retrieved {} projects from Monday.com", projects.size());
            
        } catch (Exception e) {
            log.error("Error fetching projects from Monday.com", e);
        }
        
        return projects;
    }
    
    private String buildGraphQLQuery(String cursor) {
        String cursorParam = cursor != null ? ", cursor: \"" + cursor + "\"" : "";
        
        return String.format("""
            {
              boards(ids: [%s]) {
                id
                name
                groups {
                  id
                  title
                }
                items_page(limit: 100%s) {
                  cursor
                  items {
                    id
                    name
                    group {
                      id
                      title
                    }
                    column_values {
                      id
                      text
                      value
                    }
                    subitems {
                      id
                      name
                      column_values {
                        id
                        text
                        value
                      }
                    }
                  }
                }
              }
            }
            """, mondayConfig.getBoardId(), cursorParam);
    }
    
    private JsonNode executeGraphQLQuery(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", mondayConfig.getApiKey());
            
            Map<String, String> body = new HashMap<>();
            body.put("query", query);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                MONDAY_API_URL,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            return objectMapper.readTree(response.getBody());
            
        } catch (Exception e) {
            log.error("Error executing GraphQL query", e);
            return null;
        }
    }
    
    private MondayProject parseProjectItem(JsonNode item) {
        try {
            String groupTitle = item.path("group").path("title").asText("");
            
            // Filter by configured groups
            if (!mondayConfig.getGroups().contains(groupTitle)) {
                return null;
            }
            
            MondayProject project = new MondayProject();
            project.setItemId(item.path("id").asText());
            project.setProjectName(item.path("name").asText());
            project.setGroup(groupTitle);
            
            // Parse column values
            JsonNode columnValues = item.path("column_values");
            for (JsonNode col : columnValues) {
                String colId = col.path("id").asText();
                String text = col.path("text").asText("");
                String value = col.path("value").asText("");
                
                if (!text.isEmpty()) {
                    project.getColumns().put(colId, text);
                }
                
                // Status
                if ("status".equals(colId) && !text.isEmpty()) {
                    project.setStatus(text);
                }
                
                // Developer Team (status_150)
                if ("status_150".equals(colId) && !text.isEmpty()) {
                    String[] teams = text.split(",");
                    project.setDeveloperTeam(teams[0].trim());
                }
                
                // Dev Resource (people column)
                else if ("people".equals(colId) && !text.isEmpty()) {
                    String[] devNames = text.split(",");
                    List<String> developers = new ArrayList<>();
                    for (String name : devNames) {
                        String normalized = mondayConfig.getNormalizedDeveloperName(name.trim());
                        developers.add(normalized);
                    }
                    project.setDevelopers(developers);
                }
                
                // Developer Timeline (timeline_1__1)
                else if ("timeline_1__1".equals(colId) && !value.isEmpty() && !"{}".equals(value)) {
                    try {
                        JsonNode timeline = objectMapper.readTree(value);
                        if (timeline.has("from") && timeline.has("to")) {
                            MondayProject.Timeline tl = new MondayProject.Timeline();
                            tl.setStartDate(timeline.path("from").asText());
                            tl.setEndDate(timeline.path("to").asText());
                            project.setTimeline(tl);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing timeline for project {}", project.getProjectName());
                    }
                }
                
                // Project # - SN (Link) (link_to_item__1)
                else if ("link_to_item__1".equals(colId) && !text.isEmpty()) {
                    project.setPrjNumber(text.length() > 10 ? text.substring(0, 10) : text);
                }
            }
            
            // Parse subitems
            JsonNode subitemsNode = item.path("subitems");
            if (subitemsNode.isArray() && subitemsNode.size() > 0) {
                for (JsonNode subitemNode : subitemsNode) {
                    MondaySubitem subitem = parseSubitem(subitemNode, groupTitle);
                    if (subitem != null) {
                        project.getSubitems().add(subitem);
                    }
                }
            }
            
            return project;
            
        } catch (Exception e) {
            log.error("Error parsing project item", e);
            return null;
        }
    }
    
    private MondaySubitem parseSubitem(JsonNode subitemNode, String inheritedGroup) {
        try {
            MondaySubitem subitem = new MondaySubitem();
            subitem.setSubitemId(subitemNode.path("id").asText());
            subitem.setSubitemName(subitemNode.path("name").asText());
            
            // Parse column values
            JsonNode columnValues = subitemNode.path("column_values");
            for (JsonNode col : columnValues) {
                String colId = col.path("id").asText();
                String text = col.path("text").asText("");
                String value = col.path("value").asText("");
                
                if (!text.isEmpty()) {
                    subitem.getColumns().put(colId, text);
                }
                
                // Status
                if ("status".equals(colId) && !text.isEmpty()) {
                    subitem.setStatus(text);
                }
                
                // Dev Resource (people column - "person" on subitems)
                else if ("person".equals(colId) && !text.isEmpty()) {
                    String[] devNames = text.split(",");
                    List<String> developers = new ArrayList<>();
                    for (String name : devNames) {
                        String normalized = mondayConfig.getNormalizedDeveloperName(name.trim());
                        developers.add(normalized);
                    }
                    subitem.setDevelopers(developers);
                }
                
                // Developer Timeline (timeline_1__1)
                else if ("timeline_1__1".equals(colId) && !value.isEmpty() && !"{}".equals(value)) {
                    try {
                        JsonNode timeline = objectMapper.readTree(value);
                        if (timeline.has("from") && timeline.has("to")) {
                            MondaySubitem.Timeline tl = new MondaySubitem.Timeline();
                            tl.setStartDate(timeline.path("from").asText());
                            tl.setEndDate(timeline.path("to").asText());
                            subitem.setDevTimeline(tl);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing dev timeline for subitem {}", subitem.getSubitemName());
                    }
                }
                
                // QA Timeline (timeline_2__1 on subitems, not timeline_14__1)
                else if ("timeline_2__1".equals(colId) && !value.isEmpty() && !"{}".equals(value)) {
                    try {
                        JsonNode timeline = objectMapper.readTree(value);
                        if (timeline.has("from") && timeline.has("to")) {
                            MondaySubitem.Timeline tl = new MondaySubitem.Timeline();
                            tl.setStartDate(timeline.path("from").asText());
                            tl.setEndDate(timeline.path("to").asText());
                            subitem.setQaTimeline(tl);
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing QA timeline for subitem {}", subitem.getSubitemName());
                    }
                }
                
                // Target Deployment (date__1)
                else if ("date__1".equals(colId) && !text.isEmpty()) {
                    subitem.setTargetDeploymentDate(text);
                }
                
                // Estimate (Days) - priority2 column
                else if ("priority2".equals(colId) && !text.isEmpty()) {
                    try {
                        subitem.setEstimatedDays(Integer.parseInt(text));
                    } catch (NumberFormatException e) {
                        log.warn("Error parsing estimated days for subitem {}: {}", subitem.getSubitemName(), text);
                    }
                }
            }
            
            return subitem;
            
        } catch (Exception e) {
            log.error("Error parsing subitem", e);
            return null;
        }
    }
}
