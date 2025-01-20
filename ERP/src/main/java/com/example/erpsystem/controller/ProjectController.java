package com.example.erpsystem.controller;

import com.example.erpsystem.model.*;
import com.example.erpsystem.repository.UserRepository;
import com.example.erpsystem.service.*;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
	private final MilestoneService milestoneService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserRepository userRepository;  // Add this

    public ProjectController(ProjectService projectService, 
                           TaskService taskService,
                           UserRepository userRepository,
                           MilestoneService milestoneService) {  // Add this
        this.projectService = projectService;
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.milestoneService = milestoneService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Project> createProject(@RequestBody Project project, 
                                               @RequestParam Long managerId) {
        return ResponseEntity.ok(projectService.createProject(project, managerId));
    }

    @PostMapping("/{projectId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @projectService.isProjectManager(#projectId, authentication.principal)")
    public ResponseEntity<String> addTeamMember(@PathVariable Long projectId,
                                              @PathVariable Long userId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        projectService.addTeamMember(projectId, userId, currentUser);
        return ResponseEntity.ok("Team member added successfully");
    }

    @PostMapping("/{projectId}/tasks")
    @PreAuthorize("@projectService.isTeamMember(#projectId, authentication.principal)")
    public ResponseEntity<Task> createTask(@PathVariable Long projectId,
                                         @RequestBody Task task) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        return ResponseEntity.ok(taskService.createTask(task, projectId, currentUser.getId()));
    }

    @PutMapping("/tasks/{taskId}/assign/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @projectService.isProjectManager(#taskId, authentication.principal)")
    public ResponseEntity<Task> assignTask(@PathVariable Long taskId,
                                         @PathVariable Long userId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        return ResponseEntity.ok(taskService.assignTask(taskId, userId, currentUser));
    }

    @PutMapping("/tasks/{taskId}/status")
    @PreAuthorize("hasRole('ADMIN') or @taskService.isAssignedUser(#taskId, authentication.principal)")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable Long taskId,
                                               @RequestParam Task.TaskStatus status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        return ResponseEntity.ok(taskService.updateTaskStatus(taskId, status, currentUser));
    }
    @PostMapping("/tasks/{taskId}/comments")
    @PreAuthorize("@projectService.isTeamMember(#taskId, authentication.principal)")
    public ResponseEntity<TaskComment> addTaskComment(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> payload) {  // Changed to Map
            
        if (!payload.containsKey("content")) {
            throw new IllegalArgumentException("Comment content is required");
        }
        
        String content = payload.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        return ResponseEntity.ok(taskService.addTaskComment(taskId, content, currentUser));
    }

    @PostMapping("/tasks/{taskId}/dependencies/{dependencyTaskId}")
    @PreAuthorize("hasRole('ADMIN') or @projectService.isProjectManager(#taskId, authentication.principal)")
    public ResponseEntity<Task> addTaskDependency(
            @PathVariable Long taskId,
            @PathVariable Long dependencyTaskId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        return ResponseEntity.ok(taskService.addTaskDependency(taskId, dependencyTaskId, currentUser));
    }@PostMapping("/{projectId}/milestones")
    @PreAuthorize("hasRole('ADMIN') or @projectService.isProjectManager(#projectId, authentication.principal)")
    public ResponseEntity<Milestone> createMilestone(@PathVariable Long projectId,
                                                   @RequestBody Milestone milestone) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        return ResponseEntity.ok(milestoneService.createMilestone(milestone, projectId, currentUser));
    }

    @PutMapping("/milestones/{milestoneId}/status")
    @PreAuthorize("hasRole('ADMIN') or @projectService.isProjectManager(#projectId, authentication.principal)")
    public ResponseEntity<Milestone> updateMilestoneStatus(@PathVariable Long milestoneId,
                                                         @RequestParam Milestone.MilestoneStatus status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        return ResponseEntity.ok(milestoneService.updateMilestoneStatus(milestoneId, status, currentUser));
    }
}