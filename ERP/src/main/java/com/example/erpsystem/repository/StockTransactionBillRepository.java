package com.example.erpsystem.repository;

import com.example.erpsystem.model.StockTransactionBill;
import com.example.erpsystem.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockTransactionBillRepository extends JpaRepository<StockTransactionBill, Long> {
    Optional<StockTransactionBill> findByStockTransaction(StockTransaction stockTransaction);
}
