package com.example.scheduler.service;

import com.example.scheduler.model.ProjectDependency;
import com.example.scheduler.repository.ProjectDependencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DependencyGraphService {
    
    @Autowired
    private ProjectDependencyRepository dependencyRepository;
    
    /**
     * Check if adding a dependency would create a circular reference.
     * Traverse from successor to see if predecessor is reachable.
     */
    public boolean wouldCreateCircularDependency(Integer predecessorId, Integer successorId) {
        if (predecessorId.equals(successorId)) {
            return true;
        }
        
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(successorId);
        
        while (!queue.isEmpty()) {
            Integer currentId = queue.poll();
            
            if (visited.contains(currentId)) {
                continue;
            }
            visited.add(currentId);
            
            // If we reach the predecessor, there's a circular dependency
            if (currentId.equals(predecessorId)) {
                return true;
            }
            
            // Get all successors of current node and add to queue
            List<ProjectDependency> dependencies = dependencyRepository.findByPredecessorProjectsId(currentId);
            for (ProjectDependency dep : dependencies) {
                Integer nextId = dep.getSuccessor().getProjectsId();
                if (!visited.contains(nextId)) {
                    queue.add(nextId);
                }
            }
        }
        
        return false;
    }
}
