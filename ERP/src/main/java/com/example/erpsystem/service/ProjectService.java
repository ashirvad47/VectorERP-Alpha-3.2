package com.example.erpsystem.service;

import com.example.erpsystem.model.*;
import com.example.erpsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Project createProject(Project project, Long managerId) {
        User manager = userRepository.findById(managerId)
            .orElseThrow(() -> new RuntimeException("Manager not found"));
            
        // Verify manager is an admin
        if (!hasAdminRole(manager)) {
            throw new RuntimeException("Only administrators can manage projects");
        }

        project.setProjectManager(manager);
        project.getTeamMembers().add(manager);
        return projectRepository.save(project);
    }

    @Transactional
    public void addTeamMember(Long projectId, Long userId, User requestingUser) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
            
        // Only project manager or admins can add team members
        if (!hasAdminRole(requestingUser) && !project.getProjectManager().equals(requestingUser)) {
            throw new RuntimeException("Unauthorized to modify project team");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        project.getTeamMembers().add(user);
        projectRepository.save(project);
    }

    private boolean hasAdminRole(User user) {
        return user.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ADMIN"));
    }
}

