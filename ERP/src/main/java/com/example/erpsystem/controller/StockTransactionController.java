package com.example.erpsystem.controller;

import com.example.erpsystem.model.Invoice;
import com.example.erpsystem.model.Product;
import com.example.erpsystem.model.StockTransaction;
import com.example.erpsystem.repository.InvoiceRepository;
import com.example.erpsystem.service.StockTransactionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/transactions")
public class StockTransactionController {

    private final StockTransactionService transactionService;
    private final InvoiceRepository invoiceRepository;

    public StockTransactionController(StockTransactionService transactionService, InvoiceRepository invoiceRepository) {
        this.transactionService = transactionService;
        this.invoiceRepository = invoiceRepository;
    }

    @PostMapping
    public ResponseEntity<?> logTransaction(@RequestBody Map<String, Object> transactionData) {
        try {
            // Step 1: Validate transaction data
            StockTransaction transaction = mapAndValidateTransaction(transactionData);
            
         // Step 2: Extract invoice if provided
            Invoice invoice = null;
            if (transactionData.containsKey("invoiceId")) {
                Long invoiceId = Long.valueOf(transactionData.get("invoiceId").toString());
                invoice = invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new RuntimeException("Invoice not found"));
            }
            
            // Step 2: Log the stock transaction
            StockTransaction savedTransaction = transactionService.logTransaction(
                transaction.getProduct().getId(),
                transaction.getQuantity(),
                transaction.getType(),
                transaction.getPricePerUnit(),
                invoice
            );

            return ResponseEntity.ok(savedTransaction);

        } catch (IllegalArgumentException e) {
            // Handle validation errors
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Handle general errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("An error occurred while logging the transaction: " + e.getMessage());
        }
    }

    // Helper method to map and validate transaction data
    private StockTransaction mapAndValidateTransaction(Map<String, Object> transactionData) {
        try {
            // Step 1: Validate product ID
            if (!transactionData.containsKey("productId")) {
                throw new IllegalArgumentException("Product ID is required.");
            }
            Long productId = Long.valueOf(transactionData.get("productId").toString());
            if (productId <= 0) {
                throw new IllegalArgumentException("Product ID must be a positive number.");
            }

            // Step 2: Validate quantity
            if (!transactionData.containsKey("quantity")) {
                throw new IllegalArgumentException("Quantity is required.");
            }
            Integer quantity = Integer.valueOf(transactionData.get("quantity").toString());
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero.");
            }

            // Step 3: Validate type
            if (!transactionData.containsKey("type")) {
                throw new IllegalArgumentException("Transaction type is required.");
            }
            String type = transactionData.get("type").toString();
            if (!type.equals("IN") && !type.equals("OUT")) {
                throw new IllegalArgumentException("Transaction type must be either 'IN' or 'OUT'.");
            }

            // Step 4: Validate price per unit
            if (!transactionData.containsKey("pricePerUnit")) {
                throw new IllegalArgumentException("Price per unit is required.");
            }
            BigDecimal pricePerUnit = new BigDecimal(transactionData.get("pricePerUnit").toString());
            if (pricePerUnit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price per unit must be a positive number.");
            }

            // Step 5: Create and return a validated StockTransaction object
            StockTransaction transaction = new StockTransaction();
            Product product = new Product();
            product.setId(productId);
            transaction.setProduct(product);
            transaction.setQuantity(quantity);
            transaction.setType(type);
            transaction.setPricePerUnit(pricePerUnit);

            return transaction;

        } catch (NumberFormatException | ClassCastException e) {
            throw new IllegalArgumentException("Invalid input format: " + e.getMessage());
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockTransaction>> getTransactionsByProduct(@PathVariable Long productId) {
        try {
            List<StockTransaction> transactions = transactionService.getTransactionsByProduct(productId);
            if (transactions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<StockTransaction>> getAllTransactions() {
        try {
            List<StockTransaction> transactions = transactionService.getAllTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }
    }

    @GetMapping("/bill/{transactionId}")
    public ResponseEntity<String> generateStockTransactionBill(@PathVariable Long transactionId) {
        try {
            String billFilePath = transactionService.generateBillForTransaction(transactionId);
            File billFile = new File(billFilePath);

            if (billFile.exists()) {
                return ResponseEntity.ok("Bill generated successfully. Path: " + billFilePath);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                     .body("Bill generation completed but file not found at: " + billFilePath);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error generating bill: " + e.getMessage());
        }
    }
}


