package com.example.erpsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.erpsystem.model.Milestone;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

}
