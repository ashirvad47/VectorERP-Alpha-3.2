package com.example.erpsystem.repository;

import com.example.erpsystem.model.Invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
//	List<StockTransaction> findByInvoice(Invoice invoice);
}
