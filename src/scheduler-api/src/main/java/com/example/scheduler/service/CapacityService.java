package com.example.scheduler.service;

import com.example.scheduler.dto.DeveloperCapacity;
import com.example.scheduler.dto.TeamCapacityResponse;
import com.example.scheduler.dto.WeekCapacity;
import com.example.scheduler.model.Assignment;
import com.example.scheduler.model.Developer;
import com.example.scheduler.model.ScenarioAssignment;
import com.example.scheduler.model.Team;
import com.example.scheduler.repository.AssignmentRepository;
import com.example.scheduler.repository.DeveloperRepository;
import com.example.scheduler.repository.ScenarioAssignmentRepository;
import com.example.scheduler.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class CapacityService {
    
    private static final int MAX_HOURS_PER_WEEK = 32;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private DeveloperRepository developerRepository;
    
    @Autowired
    private TeamRepository teamRepository;
    
    @Autowired
    private ScenarioAssignmentRepository scenarioAssignmentRepository;
    
    public ConflictCheckResult checkConflict(
            Integer developerId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal ratio,
            Integer excludeAssignmentId) {
        
        if (startDate == null || endDate == null || developerId == null) {
            return new ConflictCheckResult(false, new ArrayList<>());
        }
        
        // Fetch all existing assignments for the developer
        List<Assignment> assignments = assignmentRepository.findByDeveloperIdWithDetails(developerId);
        
        // Build map of week start dates to total hours
        Map<LocalDate, BigDecimal> weeklyHours = new HashMap<>();
        
        for (Assignment assignment : assignments) {
            // Skip the assignment being edited
            if (excludeAssignmentId != null && 
                assignment.getAssignmentsId().equals(excludeAssignmentId)) {
                continue;
            }
            
            if (assignment.getStartDate() != null && 
                assignment.getEndDate() != null && 
                assignment.getRatio() != null) {
                
                addAssignmentToWeeks(weeklyHours, 
                    assignment.getStartDate(), 
                    assignment.getEndDate(), 
                    assignment.getRatio());
            }
        }
        
        // Add the proposed assignment
        Map<LocalDate, BigDecimal> proposedWeeklyHours = new HashMap<>();
        addAssignmentToWeeks(proposedWeeklyHours, startDate, endDate, ratio);
        
        // Check for conflicts
        List<WeekConflict> conflicts = new ArrayList<>();
        boolean hasConflict = false;
        
        for (Map.Entry<LocalDate, BigDecimal> entry : proposedWeeklyHours.entrySet()) {
            LocalDate weekStart = entry.getKey();
            BigDecimal newHours = entry.getValue();
            BigDecimal currentHours = weeklyHours.getOrDefault(weekStart, BigDecimal.ZERO);
            BigDecimal totalHours = currentHours.add(newHours);
            
            BigDecimal capacityHours = new BigDecimal(MAX_HOURS_PER_WEEK);
            
            if (totalHours.compareTo(capacityHours) > 0) {
                hasConflict = true;
                BigDecimal overageHours = totalHours.subtract(capacityHours);
                
                conflicts.add(new WeekConflict(
                    weekStart,
                    currentHours,
                    newHours,
                    totalHours,
                    capacityHours,
                    overageHours
                ));
            }
        }
        
        return new ConflictCheckResult(hasConflict, conflicts);
    }
    
    /**
     * Get team capacity aggregated by week for the given date range.
     */
    public TeamCapacityResponse getTeamCapacity(Integer teamId, LocalDate from, LocalDate to) {
        // Get team info
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (!teamOpt.isPresent()) {
            return null;
        }
        Team team = teamOpt.get();
        
        // Get all developers in the team
        List<Developer> developers = developerRepository.findByTeamsId(teamId);
        int developerCount = developers.size();
        
        if (developerCount == 0) {
            return new TeamCapacityResponse(
                teamId, 
                team.getTeamName(), 
                new ArrayList<>(), 
                BigDecimal.ZERO, 
                0
            );
        }
        
        // Build weekly capacity data
        List<WeekCapacity> weeks = new ArrayList<>();
        BigDecimal totalUtilization = BigDecimal.ZERO;
        int weekCount = 0;
        
        LocalDate currentWeekStart = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endWeekStart = to.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        while (!currentWeekStart.isAfter(endWeekStart)) {
            BigDecimal assignedHours = BigDecimal.ZERO;
            
            // Sum hours for all developers in this week
            for (Developer developer : developers) {
                List<Assignment> assignments = assignmentRepository.findByDeveloperIdWithDetails(developer.getDevelopersId());
                
                for (Assignment assignment : assignments) {
                    if (assignment.getStartDate() != null && 
                        assignment.getEndDate() != null && 
                        assignment.getRatio() != null) {
                        
                        // Check if assignment overlaps with this week
                        LocalDate assignmentStart = assignment.getStartDate();
                        LocalDate assignmentEnd = assignment.getEndDate();
                        LocalDate weekEnd = currentWeekStart.plusDays(6);
                        
                        if (!assignmentEnd.isBefore(currentWeekStart) && !assignmentStart.isAfter(weekEnd)) {
                            BigDecimal hours = assignment.getRatio().multiply(new BigDecimal(MAX_HOURS_PER_WEEK));
                            assignedHours = assignedHours.add(hours);
                        }
                    }
                }
            }
            
            BigDecimal totalCapacity = new BigDecimal(MAX_HOURS_PER_WEEK * developerCount);
            BigDecimal availableHours = totalCapacity.subtract(assignedHours);
            BigDecimal utilization = totalCapacity.compareTo(BigDecimal.ZERO) > 0 
                ? assignedHours.divide(totalCapacity, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;
            
            weeks.add(new WeekCapacity(
                currentWeekStart,
                totalCapacity,
                assignedHours,
                availableHours,
                utilization,
                developerCount
            ));
            
            totalUtilization = totalUtilization.add(utilization);
            weekCount++;
            currentWeekStart = currentWeekStart.plusWeeks(1);
        }
        
        BigDecimal averageUtilization = weekCount > 0 
            ? totalUtilization.divide(new BigDecimal(weekCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return new TeamCapacityResponse(
            teamId,
            team.getTeamName(),
            weeks,
            averageUtilization,
            developerCount
        );
    }
    
    /**
     * Get capacity for all teams in the given date range.
     */
    public List<TeamCapacityResponse> getAllTeamsCapacity(LocalDate from, LocalDate to) {
        List<Team> teams = teamRepository.findAll();
        List<TeamCapacityResponse> responses = new ArrayList<>();
        
        for (Team team : teams) {
            TeamCapacityResponse response = getTeamCapacity(team.getTeamId(), from, to);
            if (response != null) {
                responses.add(response);
            }
        }
        
        return responses;
    }
    
    /**
     * Get per-developer breakdown for a team in the given date range.
     */
    public List<DeveloperCapacity> getTeamDeveloperBreakdown(Integer teamId, LocalDate from, LocalDate to) {
        List<Developer> developers = developerRepository.findByTeamsId(teamId);
        List<DeveloperCapacity> result = new ArrayList<>();
        
        LocalDate currentWeekStart = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endWeekStart = to.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        for (Developer developer : developers) {
            Map<LocalDate, BigDecimal> weeklyHours = new LinkedHashMap<>();
            List<Assignment> assignments = assignmentRepository.findByDeveloperIdWithDetails(developer.getDevelopersId());
            
            LocalDate weekStart = currentWeekStart;
            while (!weekStart.isAfter(endWeekStart)) {
                BigDecimal hoursThisWeek = BigDecimal.ZERO;
                
                for (Assignment assignment : assignments) {
                    if (assignment.getStartDate() != null && 
                        assignment.getEndDate() != null && 
                        assignment.getRatio() != null) {
                        
                        LocalDate assignmentStart = assignment.getStartDate();
                        LocalDate assignmentEnd = assignment.getEndDate();
                        LocalDate weekEnd = weekStart.plusDays(6);
                        
                        if (!assignmentEnd.isBefore(weekStart) && !assignmentStart.isAfter(weekEnd)) {
                            BigDecimal hours = assignment.getRatio().multiply(new BigDecimal(MAX_HOURS_PER_WEEK));
                            hoursThisWeek = hoursThisWeek.add(hours);
                        }
                    }
                }
                
                weeklyHours.put(weekStart, hoursThisWeek);
                weekStart = weekStart.plusWeeks(1);
            }
            
            result.add(new DeveloperCapacity(
                developer.getDevelopersId(),
                developer.getFullName(),
                developer.getPosition(),
                weeklyHours
            ));
        }
        
        return result;
    }
    
    private void addAssignmentToWeeks(
            Map<LocalDate, BigDecimal> weeklyHours,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal ratio) {
        
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            
            BigDecimal hours = ratio.multiply(new BigDecimal(MAX_HOURS_PER_WEEK));
            weeklyHours.merge(weekStart, hours, BigDecimal::add);
            
            currentDate = currentDate.plusWeeks(1);
        }
    }
    
    /**
     * Build effective assignment list by overlaying scenario changes on live assignments.
     */
    private List<Assignment> buildEffectiveAssignmentList(Integer scenarioId) {
        List<Assignment> liveAssignments = new ArrayList<>(assignmentRepository.findAllWithDetails());
        List<ScenarioAssignment> changes = scenarioAssignmentRepository.findByScenarioScenarioId(scenarioId);
        
        // Track which original assignments have been modified or deleted
        Set<Integer> deletedIds = new HashSet<>();
        Map<Integer, ScenarioAssignment> modifyMap = new HashMap<>();
        
        // First pass: collect DELETEs and MODIFYs
        for (ScenarioAssignment change : changes) {
            if ("DELETE".equals(change.getChangeType()) && change.getOriginalAssignmentId() != null) {
                deletedIds.add(change.getOriginalAssignmentId());
            } else if ("MODIFY".equals(change.getChangeType()) && change.getOriginalAssignmentId() != null) {
                modifyMap.put(change.getOriginalAssignmentId(), change);
            }
        }
        
        // Apply DELETEs
        liveAssignments.removeIf(a -> deletedIds.contains(a.getAssignmentsId()));
        
        // Apply MODIFYs
        for (Assignment assignment : liveAssignments) {
            if (modifyMap.containsKey(assignment.getAssignmentsId())) {
                ScenarioAssignment change = modifyMap.get(assignment.getAssignmentsId());
                if (change.getProject() != null) assignment.setProject(change.getProject());
                if (change.getStartDate() != null) assignment.setStartDate(change.getStartDate());
                if (change.getEndDate() != null) assignment.setEndDate(change.getEndDate());
                if (change.getRatio() != null) assignment.setRatio(change.getRatio());
                if (change.getDeveloper() != null) assignment.setDeveloper(change.getDeveloper());
            }
        }
        
        // Add ADDs
        for (ScenarioAssignment change : changes) {
            if ("ADD".equals(change.getChangeType())) {
                Assignment virtualAssignment = new Assignment();
                virtualAssignment.setAssignmentsId(-1); // Virtual ID
                virtualAssignment.setProject(change.getProject());
                virtualAssignment.setDeveloper(change.getDeveloper());
                virtualAssignment.setStartDate(change.getStartDate());
                virtualAssignment.setEndDate(change.getEndDate());
                virtualAssignment.setRatio(change.getRatio());
                liveAssignments.add(virtualAssignment);
            }
        }
        
        return liveAssignments;
    }
    
    /**
     * Get team capacity with scenario overlay.
     */
    public TeamCapacityResponse getTeamCapacityWithScenario(
            Integer teamId, LocalDate from, LocalDate to, Integer scenarioId) {
        
        // Get team info
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (!teamOpt.isPresent()) {
            return null;
        }
        Team team = teamOpt.get();
        
        // Get all developers in the team
        List<Developer> developers = developerRepository.findByTeamsId(teamId);
        int developerCount = developers.size();
        
        if (developerCount == 0) {
            return new TeamCapacityResponse(
                teamId, 
                team.getTeamName(), 
                new ArrayList<>(), 
                BigDecimal.ZERO, 
                0
            );
        }
        
        // Build effective assignments with scenario overlay
        List<Assignment> effectiveAssignments = buildEffectiveAssignmentList(scenarioId);
        
        // Build weekly capacity data using effective assignments
        List<WeekCapacity> weeks = new ArrayList<>();
        BigDecimal totalUtilization = BigDecimal.ZERO;
        int weekCount = 0;
        
        LocalDate currentWeekStart = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endWeekStart = to.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        while (!currentWeekStart.isAfter(endWeekStart)) {
            BigDecimal assignedHours = BigDecimal.ZERO;
            
            // Sum hours for all developers in this week using effective assignments
            for (Developer developer : developers) {
                for (Assignment assignment : effectiveAssignments) {
                    if (assignment.getDeveloper() != null &&
                        assignment.getDeveloper().getDevelopersId().equals(developer.getDevelopersId()) &&
                        assignment.getStartDate() != null && 
                        assignment.getEndDate() != null && 
                        assignment.getRatio() != null) {
                        
                        LocalDate assignmentStart = assignment.getStartDate();
                        LocalDate assignmentEnd = assignment.getEndDate();
                        LocalDate weekEnd = currentWeekStart.plusDays(6);
                        
                        if (!assignmentEnd.isBefore(currentWeekStart) && !assignmentStart.isAfter(weekEnd)) {
                            BigDecimal hours = assignment.getRatio().multiply(new BigDecimal(MAX_HOURS_PER_WEEK));
                            assignedHours = assignedHours.add(hours);
                        }
                    }
                }
            }
            
            BigDecimal totalCapacity = new BigDecimal(MAX_HOURS_PER_WEEK * developerCount);
            BigDecimal availableHours = totalCapacity.subtract(assignedHours);
            BigDecimal utilization = totalCapacity.compareTo(BigDecimal.ZERO) > 0 
                ? assignedHours.divide(totalCapacity, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100))
                : BigDecimal.ZERO;
            
            weeks.add(new WeekCapacity(
                currentWeekStart,
                totalCapacity,
                assignedHours,
                availableHours,
                utilization,
                developerCount
            ));
            
            totalUtilization = totalUtilization.add(utilization);
            weekCount++;
            currentWeekStart = currentWeekStart.plusWeeks(1);
        }
        
        BigDecimal averageUtilization = weekCount > 0 
            ? totalUtilization.divide(new BigDecimal(weekCount), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        return new TeamCapacityResponse(
            teamId,
            team.getTeamName(),
            weeks,
            averageUtilization,
            developerCount
        );
    }
    
    /**
     * Get all teams capacity with scenario overlay.
     */
    public List<TeamCapacityResponse> getAllTeamsCapacityWithScenario(
            LocalDate from, LocalDate to, Integer scenarioId) {
        
        List<Team> teams = teamRepository.findAll();
        List<TeamCapacityResponse> responses = new ArrayList<>();
        
        for (Team team : teams) {
            TeamCapacityResponse response = getTeamCapacityWithScenario(team.getTeamId(), from, to, scenarioId);
            if (response != null) {
                responses.add(response);
            }
        }
        
        return responses;
    }
    
    /**
     * Get per-developer breakdown for a team with scenario overlay.
     */
    public List<DeveloperCapacity> getTeamDeveloperBreakdownWithScenario(
            Integer teamId, LocalDate from, LocalDate to, Integer scenarioId) {
        
        List<Developer> developers = developerRepository.findByTeamsId(teamId);
        List<DeveloperCapacity> result = new ArrayList<>();
        
        // Build effective assignments with scenario overlay
        List<Assignment> effectiveAssignments = buildEffectiveAssignmentList(scenarioId);
        
        LocalDate currentWeekStart = from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endWeekStart = to.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        for (Developer developer : developers) {
            Map<LocalDate, BigDecimal> weeklyHours = new LinkedHashMap<>();
            
            LocalDate weekStart = currentWeekStart;
            while (!weekStart.isAfter(endWeekStart)) {
                BigDecimal hoursThisWeek = BigDecimal.ZERO;
                
                for (Assignment assignment : effectiveAssignments) {
                    if (assignment.getDeveloper() != null &&
                        assignment.getDeveloper().getDevelopersId().equals(developer.getDevelopersId()) &&
                        assignment.getStartDate() != null && 
                        assignment.getEndDate() != null && 
                        assignment.getRatio() != null) {
                        
                        LocalDate assignmentStart = assignment.getStartDate();
                        LocalDate assignmentEnd = assignment.getEndDate();
                        LocalDate weekEnd = weekStart.plusDays(6);
                        
                        if (!assignmentEnd.isBefore(weekStart) && !assignmentStart.isAfter(weekEnd)) {
                            BigDecimal hours = assignment.getRatio().multiply(new BigDecimal(MAX_HOURS_PER_WEEK));
                            hoursThisWeek = hoursThisWeek.add(hours);
                        }
                    }
                }
                
                weeklyHours.put(weekStart, hoursThisWeek);
                weekStart = weekStart.plusWeeks(1);
            }
            
            result.add(new DeveloperCapacity(
                developer.getDevelopersId(),
                developer.getFullName(),
                developer.getPosition(),
                weeklyHours
            ));
        }
        
        return result;
    }
    
    public static class ConflictCheckResult {
        private boolean hasConflict;
        private List<WeekConflict> weeks;
        
        public ConflictCheckResult(boolean hasConflict, List<WeekConflict> weeks) {
            this.hasConflict = hasConflict;
            this.weeks = weeks;
        }
        
        public boolean isHasConflict() {
            return hasConflict;
        }
        
        public void setHasConflict(boolean hasConflict) {
            this.hasConflict = hasConflict;
        }
        
        public List<WeekConflict> getWeeks() {
            return weeks;
        }
        
        public void setWeeks(List<WeekConflict> weeks) {
            this.weeks = weeks;
        }
    }
    
    public static class WeekConflict {
        private LocalDate weekStart;
        private BigDecimal currentHours;
        private BigDecimal newHours;
        private BigDecimal totalHours;
        private BigDecimal capacityHours;
        private BigDecimal overageHours;
        
        public WeekConflict(LocalDate weekStart, BigDecimal currentHours, 
                          BigDecimal newHours, BigDecimal totalHours,
                          BigDecimal capacityHours, BigDecimal overageHours) {
            this.weekStart = weekStart;
            this.currentHours = currentHours;
            this.newHours = newHours;
            this.totalHours = totalHours;
            this.capacityHours = capacityHours;
            this.overageHours = overageHours;
        }
        
        public LocalDate getWeekStart() {
            return weekStart;
        }
        
        public void setWeekStart(LocalDate weekStart) {
            this.weekStart = weekStart;
        }
        
        public BigDecimal getCurrentHours() {
            return currentHours;
        }
        
        public void setCurrentHours(BigDecimal currentHours) {
            this.currentHours = currentHours;
        }
        
        public BigDecimal getNewHours() {
            return newHours;
        }
        
        public void setNewHours(BigDecimal newHours) {
            this.newHours = newHours;
        }
        
        public BigDecimal getTotalHours() {
            return totalHours;
        }
        
        public void setTotalHours(BigDecimal totalHours) {
            this.totalHours = totalHours;
        }
        
        public BigDecimal getCapacityHours() {
            return capacityHours;
        }
        
        public void setCapacityHours(BigDecimal capacityHours) {
            this.capacityHours = capacityHours;
        }
        
        public BigDecimal getOverageHours() {
            return overageHours;
        }
        
        public void setOverageHours(BigDecimal overageHours) {
            this.overageHours = overageHours;
        }
    }
}
