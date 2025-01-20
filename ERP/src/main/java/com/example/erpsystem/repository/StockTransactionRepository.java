package com.example.erpsystem.repository;

import com.example.erpsystem.model.Invoice;
import com.example.erpsystem.model.StockTransaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

	List<StockTransaction> findByInvoice(Invoice invoice);
}
