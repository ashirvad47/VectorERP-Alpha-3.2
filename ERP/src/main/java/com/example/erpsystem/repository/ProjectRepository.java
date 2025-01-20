package com.example.erpsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.erpsystem.model.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>{

}

