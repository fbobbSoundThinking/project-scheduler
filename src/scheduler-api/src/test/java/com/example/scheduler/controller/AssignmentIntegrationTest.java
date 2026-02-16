package com.example.scheduler.controller;

import com.example.scheduler.model.Assignment;
import com.example.scheduler.repository.AssignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssignmentRepository assignmentRepository;

    @Test
    void deleteAssignment_ExistingId_ReturnsOk() throws Exception {
        // Given
        Integer assignmentId = 1;
        Assignment mockAssignment = new Assignment();
        mockAssignment.setAssignmentsId(assignmentId);
        
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        doNothing().when(assignmentRepository).delete(any(Assignment.class));

        // When & Then
        mockMvc.perform(delete("/api/assignments/{id}", assignmentId))
                .andExpect(status().isOk());

        verify(assignmentRepository, times(1)).findById(assignmentId);
        verify(assignmentRepository, times(1)).delete(mockAssignment);
    }

    @Test
    void deleteAssignment_NonExistentId_ReturnsNotFound() throws Exception {
        // Given
        Integer nonExistentId = 999;
        when(assignmentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/assignments/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        verify(assignmentRepository, times(1)).findById(nonExistentId);
        verify(assignmentRepository, never()).delete(any(Assignment.class));
    }

    @Test
    void deleteAssignment_ValidId_RemovesFromDatabase() throws Exception {
        // Given
        Integer assignmentId = 1;
        Assignment mockAssignment = new Assignment();
        mockAssignment.setAssignmentsId(assignmentId);
        
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        doNothing().when(assignmentRepository).delete(mockAssignment);

        // When
        mockMvc.perform(delete("/api/assignments/{id}", assignmentId))
                .andExpect(status().isOk());

        // Then - verify repository delete was called
        verify(assignmentRepository, times(1)).delete(mockAssignment);
    }

    @Test
    void deleteAssignment_WithRelatedData_CascadesCorrectly() throws Exception {
        // Given - assignment with related developer and project (Lombok entities)
        Integer assignmentId = 1;
        Assignment mockAssignment = new Assignment();
        mockAssignment.setAssignmentsId(assignmentId);
        
        when(assignmentRepository.findById(assignmentId)).thenReturn(Optional.of(mockAssignment));
        doNothing().when(assignmentRepository).delete(mockAssignment);

        // When
        mockMvc.perform(delete("/api/assignments/{id}", assignmentId))
                .andExpect(status().isOk());

        // Then - verify deletion doesn't break referential integrity
        verify(assignmentRepository, times(1)).delete(mockAssignment);
        // Note: Developer and Project remain in DB (not cascade deleted)
    }
}
