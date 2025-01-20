package com.example.erpsystem.service;

import com.example.erpsystem.model.*;
import com.example.erpsystem.repository.TaskRepository;
import com.example.erpsystem.repository.ProjectRepository;
import com.example.erpsystem.repository.UserRepository;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, 
                       ProjectRepository projectRepository,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Task createTask(Task task, Long projectId, Long createdById) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
            
        User creator = userRepository.findById(createdById)
            .orElseThrow(() -> new RuntimeException("Creator not found"));

        // Verify creator is project team member
        if (!project.getTeamMembers().contains(creator)) {
            throw new RuntimeException("Only team members can create tasks");
        }

        task.setProject(project);
        task.setCreatedBy(creator);
        task.setStatus(Task.TaskStatus.TODO);
        return taskRepository.save(task);
    }

    @Transactional
    public Task assignTask(Long taskId, Long userId, User requestingUser) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
            
        User assignee = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Only project manager or admins can assign tasks
        if (!hasAdminRole(requestingUser) && 
            !task.getProject().getProjectManager().equals(requestingUser)) {
            throw new RuntimeException("Unauthorized to assign tasks");
        }

        // Verify assignee is project team member
        if (!task.getProject().getTeamMembers().contains(assignee)) {
            throw new RuntimeException("Can only assign tasks to team members");
        }

        task.setAssignedTo(assignee);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, Task.TaskStatus newStatus, User requestingUser) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));

        // Only assigned user or admin can update status
        if (!hasAdminRole(requestingUser) && 
            !task.getAssignedTo().equals(requestingUser)) {
            throw new RuntimeException("Unauthorized to update task status");
        }

        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    // Add this method to check admin role
    private boolean hasAdminRole(User user) {
        return user.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ADMIN"));
    }
    
    @Transactional
    public TaskComment addTaskComment(Long taskId, String content, User commenter) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));

        // Verify commenter is project team member
        if (!task.getProject().getTeamMembers().contains(commenter)) {
            throw new RuntimeException("Only team members can comment on tasks");
        }

        TaskComment comment = new TaskComment();
        comment.setContent(content);
        comment.setTask(task);
        comment.setUser (commenter);
        comment.setCreatedAt(LocalDateTime.now());

        task.getComments().add(comment);
        taskRepository.save(task);
        return comment;
    }

    @Transactional
    public Task addTaskDependency(Long taskId, Long dependencyTaskId, User requestingUser) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Task not found"));
        Task dependencyTask = taskRepository.findById(dependencyTaskId)
            .orElseThrow(() -> new RuntimeException("Dependency task not found"));

        // Only project manager or admin can manage dependencies
        if (!hasAdminRole(requestingUser) && 
            !task.getProject().getProjectManager().equals(requestingUser)) {
            throw new RuntimeException("Unauthorized to manage task dependencies");
        }

        // Verify both tasks belong to the same project
        if (!task.getProject().equals(dependencyTask.getProject())) {
            throw new RuntimeException("Cannot add dependency from different project");
        }

        task.getDependencies().add(dependencyTask);
        return taskRepository.save(task);
    }

   
}