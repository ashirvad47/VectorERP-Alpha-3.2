package com.example.erpsystem.service;

import com.example.erpsystem.model.*;
import com.example.erpsystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MilestoneService {
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;

    public MilestoneService(MilestoneRepository milestoneRepository, 
                           ProjectRepository projectRepository) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Milestone createMilestone(Milestone milestone, Long projectId, User requestingUser) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));

        // Only project manager or admins can create milestones
        if (!hasAdminRole(requestingUser) && 
            !project.getProjectManager().equals(requestingUser)) {
            throw new RuntimeException("Unauthorized to create milestones");
        }

        milestone.setProject(project);
        milestone.setStatus(Milestone.MilestoneStatus.PENDING);
        return milestoneRepository.save(milestone);
    }

    @Transactional
    public Milestone updateMilestoneStatus(Long milestoneId, 
                                         Milestone.MilestoneStatus newStatus, 
                                         User requestingUser) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new RuntimeException("Milestone not found"));

        // Only project manager or admins can update milestone status
        if (!hasAdminRole(requestingUser) && 
            !milestone.getProject().getProjectManager().equals(requestingUser)) {
            throw new RuntimeException("Unauthorized to update milestone status");
        }

        milestone.setStatus(newStatus);
        return milestoneRepository.save(milestone);
    }

    private boolean hasAdminRole(User user) {
        return user.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ADMIN"));
    }
}