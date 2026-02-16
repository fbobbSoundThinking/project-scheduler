package com.example.scheduler.service;

import com.example.scheduler.dto.MondayProject;
import com.example.scheduler.dto.MondaySubitem;
import com.example.scheduler.dto.SyncResponse;
import com.example.scheduler.model.Assignment;
import com.example.scheduler.model.Developer;
import com.example.scheduler.model.Project;
import com.example.scheduler.model.Subitem;
import com.example.scheduler.model.TeamNameMap;
import com.example.scheduler.repository.AssignmentRepository;
import com.example.scheduler.repository.DeveloperRepository;
import com.example.scheduler.repository.ProjectRepository;
import com.example.scheduler.repository.SubitemRepository;
import com.example.scheduler.repository.TeamNameMapRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class ProjectSyncService {
    
    @Autowired
    private MondayService mondayService;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private DeveloperRepository developerRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private TeamNameMapRepository teamNameMapRepository;
    
    @Autowired
    private SubitemRepository subitemRepository;
    
    @Transactional
    public SyncResponse syncProjectsFromMonday() {
        SyncResponse response = new SyncResponse();
        int projectsUpdated = 0;
        int projectsInserted = 0;
        int assignmentsUpdated = 0;
        int assignmentsInserted = 0;
        int subitemsUpdated = 0;
        int subitemsInserted = 0;
        
        try {
            log.info("Starting Monday.com sync");
            
            // Fetch projects from Monday.com
            List<MondayProject> mondayProjects = mondayService.fetchProjectsFromMonday();
            
            if (mondayProjects.isEmpty()) {
                response.setSuccess(true);
                response.setMessage("No projects found in Monday.com");
                return response;
            }
            
            // Build team name to ID map
            Map<String, Integer> teamMap = buildTeamMap();
            
            // Build developer name to entity map
            Map<String, Developer> developerMap = buildDeveloperMap();
            
            // Track project ID mappings
            Map<String, Integer> projectIdMap = new HashMap<>();
            
            // Sync projects
            for (MondayProject mondayProject : mondayProjects) {
                try {
                    Integer teamId = getTeamId(mondayProject.getDeveloperTeam(), teamMap);
                    
                    Optional<Project> existingProject = projectRepository.findByItemId(mondayProject.getItemId());
                    
                    if (existingProject.isPresent()) {
                        // Update existing project
                        Project project = existingProject.get();
                        project.setProjectName(mondayProject.getProjectName());
                        project.setStatus(mondayProject.getStatus());
                        project.setGroup(mondayProject.getGroup());
                        if (teamId != null) {
                            project.setPrimaryTeamId(teamId);
                        }
                        if (mondayProject.getPrjNumber() != null) {
                            project.setPrjNumber(mondayProject.getPrjNumber());
                        }
                        projectRepository.save(project);
                        projectIdMap.put(mondayProject.getItemId(), project.getProjectsId());
                        projectsUpdated++;
                        log.debug("Updated project: {}", project.getProjectName());
                    } else {
                        // Insert new project
                        Project project = new Project();
                        project.setItemId(mondayProject.getItemId());
                        project.setProjectName(mondayProject.getProjectName());
                        project.setStatus(mondayProject.getStatus());
                        project.setGroup(mondayProject.getGroup());
                        project.setLevel("TBD");
                        project.setPrimaryTeamId(teamId != null ? teamId : 0);
                        project.setPrimaryAppId(26);
                        project.setPrjNumber(mondayProject.getPrjNumber());
                        
                        Project savedProject = projectRepository.save(project);
                        projectIdMap.put(mondayProject.getItemId(), savedProject.getProjectsId());
                        projectsInserted++;
                        log.debug("Inserted project: {}", project.getProjectName());
                    }
                } catch (Exception e) {
                    log.error("Error syncing project: {}", mondayProject.getProjectName(), e);
                }
            }
            
            // Sync assignments
            for (MondayProject mondayProject : mondayProjects) {
                Integer projectId = projectIdMap.get(mondayProject.getItemId());
                if (projectId == null) {
                    continue;
                }
                
                if (mondayProject.getDevelopers() != null && !mondayProject.getDevelopers().isEmpty()) {
                    for (String devName : mondayProject.getDevelopers()) {
                        Developer developer = developerMap.get(devName);
                        if (developer == null) {
                            log.warn("Developer not found: {}", devName);
                            continue;
                        }
                        
                        try {
                            // Check if assignment exists
                            Optional<Assignment> existingAssignment = assignmentRepository
                                .findByProjectIdAndDeveloperId(projectId, developer.getDevelopersId());
                            
                            if (existingAssignment.isPresent()) {
                                // Update existing assignment
                                Assignment assignment = existingAssignment.get();
                                if (mondayProject.getTimeline() != null) {
                                    assignment.setStartDate(parseDate(mondayProject.getTimeline().getStartDate()));
                                    assignment.setEndDate(parseDate(mondayProject.getTimeline().getEndDate()));
                                }
                                assignmentRepository.save(assignment);
                                assignmentsUpdated++;
                            } else {
                                // Create new assignment
                                Assignment assignment = new Assignment();
                                
                                Project project = new Project();
                                project.setProjectsId(projectId);
                                assignment.setProject(project);
                                
                                assignment.setDeveloper(developer);
                                
                                if (mondayProject.getTimeline() != null) {
                                    assignment.setStartDate(parseDate(mondayProject.getTimeline().getStartDate()));
                                    assignment.setEndDate(parseDate(mondayProject.getTimeline().getEndDate()));
                                }
                                assignment.setRatio(BigDecimal.ONE);
                                
                                assignmentRepository.save(assignment);
                                assignmentsInserted++;
                            }
                        } catch (Exception e) {
                            log.error("Error syncing assignment for developer {} on project {}", 
                                devName, mondayProject.getProjectName(), e);
                        }
                    }
                }
            }
            
            // Sync subitems
            for (MondayProject mondayProject : mondayProjects) {
                Integer projectId = projectIdMap.get(mondayProject.getItemId());
                if (projectId == null || mondayProject.getSubitems() == null) {
                    continue;
                }
                
                for (MondaySubitem mondaySubitem : mondayProject.getSubitems()) {
                    try {
                        // Find or create subitem
                        Optional<Subitem> existingSubitem = subitemRepository.findBySubitemId(mondaySubitem.getSubitemId());
                        
                        Subitem subitem;
                        if (existingSubitem.isPresent()) {
                            // Update existing subitem
                            subitem = existingSubitem.get();
                            subitem.setSubitemName(mondaySubitem.getSubitemName());
                            subitem.setStatus(mondaySubitem.getStatus());
                            subitem.setGroup(mondayProject.getGroup());  // Inherit from parent
                            
                            // Update date fields
                            if (mondaySubitem.getDevTimeline() != null) {
                                subitem.setDevStartDate(parseDate(mondaySubitem.getDevTimeline().getStartDate()));
                                subitem.setDevEndDate(parseDate(mondaySubitem.getDevTimeline().getEndDate()));
                            }
                            if (mondaySubitem.getQaTimeline() != null) {
                                subitem.setQaStartDate(parseDate(mondaySubitem.getQaTimeline().getStartDate()));
                                subitem.setQaEndDate(parseDate(mondaySubitem.getQaTimeline().getEndDate()));
                            }
                            if (mondaySubitem.getTargetDeploymentDate() != null) {
                                subitem.setTargetDeploymentDate(parseDate(mondaySubitem.getTargetDeploymentDate()));
                            }
                            subitem.setEstimatedDays(mondaySubitem.getEstimatedDays());
                            
                            subitemRepository.save(subitem);
                            subitemsUpdated++;
                            log.debug("Updated subitem: {}", subitem.getSubitemName());
                        } else {
                            // Insert new subitem
                            subitem = new Subitem();
                            subitem.setSubitemId(mondaySubitem.getSubitemId());
                            subitem.setSubitemName(mondaySubitem.getSubitemName());
                            subitem.setStatus(mondaySubitem.getStatus());
                            subitem.setGroup(mondayProject.getGroup());  // Inherit from parent
                            
                            // Set date fields
                            if (mondaySubitem.getDevTimeline() != null) {
                                subitem.setDevStartDate(parseDate(mondaySubitem.getDevTimeline().getStartDate()));
                                subitem.setDevEndDate(parseDate(mondaySubitem.getDevTimeline().getEndDate()));
                            }
                            if (mondaySubitem.getQaTimeline() != null) {
                                subitem.setQaStartDate(parseDate(mondaySubitem.getQaTimeline().getStartDate()));
                                subitem.setQaEndDate(parseDate(mondaySubitem.getQaTimeline().getEndDate()));
                            }
                            if (mondaySubitem.getTargetDeploymentDate() != null) {
                                subitem.setTargetDeploymentDate(parseDate(mondaySubitem.getTargetDeploymentDate()));
                            }
                            subitem.setEstimatedDays(mondaySubitem.getEstimatedDays());
                            
                            Project project = new Project();
                            project.setProjectsId(projectId);
                            subitem.setProject(project);
                            
                            subitem = subitemRepository.save(subitem);
                            subitemsInserted++;
                            log.debug("Inserted subitem: {}", subitem.getSubitemName());
                        }
                        
                        // Sync assignments for this subitem
                        if (mondaySubitem.getDevelopers() != null && !mondaySubitem.getDevelopers().isEmpty()) {
                            for (String devName : mondaySubitem.getDevelopers()) {
                                Developer developer = developerMap.get(devName);
                                if (developer == null) {
                                    log.warn("Developer not found for subitem: {}", devName);
                                    continue;
                                }
                                
                                try {
                                    // Check if assignment exists for this subitem
                                    Optional<Assignment> existingAssignment = assignmentRepository
                                        .findBySubitemSubitemsIdAndDeveloperDevelopersId(
                                            subitem.getSubitemsId(), 
                                            developer.getDevelopersId()
                                        );
                                    
                                    if (existingAssignment.isPresent()) {
                                        // Update existing assignment
                                        Assignment assignment = existingAssignment.get();
                                        if (mondaySubitem.getDevTimeline() != null) {
                                            assignment.setStartDate(parseDate(mondaySubitem.getDevTimeline().getStartDate()));
                                            assignment.setEndDate(parseDate(mondaySubitem.getDevTimeline().getEndDate()));
                                        }
                                        assignmentRepository.save(assignment);
                                        assignmentsUpdated++;
                                    } else {
                                        // Create new assignment for subitem
                                        Assignment assignment = new Assignment();
                                        assignment.setSubitem(subitem);
                                        assignment.setDeveloper(developer);
                                        
                                        if (mondaySubitem.getDevTimeline() != null) {
                                            assignment.setStartDate(parseDate(mondaySubitem.getDevTimeline().getStartDate()));
                                            assignment.setEndDate(parseDate(mondaySubitem.getDevTimeline().getEndDate()));
                                        }
                                        assignment.setRatio(BigDecimal.ONE);
                                        
                                        assignmentRepository.save(assignment);
                                        assignmentsInserted++;
                                    }
                                } catch (Exception e) {
                                    log.error("Error syncing assignment for developer {} on subitem {}", 
                                        devName, mondaySubitem.getSubitemName(), e);
                                }
                            }
                        }
                        
                    } catch (Exception e) {
                        log.error("Error syncing subitem: {}", mondaySubitem.getSubitemName(), e);
                    }
                }
            }
            
            response.setSuccess(true);
            response.setProjectsUpdated(projectsUpdated);
            response.setProjectsInserted(projectsInserted);
            response.setAssignmentsUpdated(assignmentsUpdated);
            response.setAssignmentsInserted(assignmentsInserted);
            response.setSubitemsUpdated(subitemsUpdated);
            response.setSubitemsInserted(subitemsInserted);
            response.setMessage(String.format(
                "Sync completed: %d projects updated, %d projects inserted, %d subitems updated, %d subitems inserted, %d assignments updated, %d assignments inserted",
                projectsUpdated, projectsInserted, subitemsUpdated, subitemsInserted, assignmentsUpdated, assignmentsInserted
            ));
            
            log.info("Sync completed successfully: {}", response.getMessage());
            
        } catch (Exception e) {
            log.error("Error during Monday.com sync", e);
            response.setSuccess(false);
            response.setError(e.getMessage());
            response.setMessage("Sync failed: " + e.getMessage());
        }
        
        return response;
    }
    
    private Map<String, Integer> buildTeamMap() {
        Map<String, Integer> teamMap = new HashMap<>();
        List<TeamNameMap> teamNameMaps = teamNameMapRepository.findAll();
        for (TeamNameMap mapping : teamNameMaps) {
            teamMap.put(mapping.getMondayTeamName(), mapping.getTeamsId());
        }
        log.debug("Built team map with {} entries", teamMap.size());
        return teamMap;
    }
    
    private Map<String, Developer> buildDeveloperMap() {
        Map<String, Developer> developerMap = new HashMap<>();
        List<Developer> developers = developerRepository.findAll();
        for (Developer dev : developers) {
            String fullName = (dev.getFirstName() + " " + dev.getLastName()).toUpperCase();
            developerMap.put(fullName, dev);
        }
        return developerMap;
    }
    
    private Integer getTeamId(String teamName, Map<String, Integer> teamMap) {
        if (teamName == null || teamName.isEmpty()) {
            return null;
        }
        return teamMap.get(teamName);
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            log.warn("Error parsing date: {}", dateStr);
            return null;
        }
    }
}
