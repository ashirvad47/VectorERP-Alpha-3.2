package com.example.erpsystem.repository;

import com.example.erpsystem.model.Transaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	 List<Transaction> findByType(String type);

}




