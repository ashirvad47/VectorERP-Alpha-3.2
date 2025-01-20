package com.example.erpsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.erpsystem.model.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>{


}
