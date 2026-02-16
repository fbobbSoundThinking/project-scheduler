package com.example.scheduler.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SyncResponse {
    private boolean success;
    private int projectsUpdated;
    private int projectsInserted;
    private int assignmentsUpdated;
    private int assignmentsInserted;
    private int subitemsUpdated;
    private int subitemsInserted;
    private String message;
    private String error;
}
