package com.example.erpsystem.controller;

import com.example.erpsystem.model.Invoice;
import com.example.erpsystem.model.Product;
import com.example.erpsystem.model.StockTransaction;
import com.example.erpsystem.repository.ProductRepository;
import com.example.erpsystem.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/generate")
    public ResponseEntity<?> generateInvoice(@RequestBody Map<String, Object> requestBody) {
        try {
            // Step 1: Validate request body
            if (!requestBody.containsKey("customerName") || !requestBody.containsKey("stockTransactions")) {
                return ResponseEntity.badRequest().body("Missing required fields: 'customerName' or 'stockTransactions'");
            }

            String customerName = (String) requestBody.get("customerName");
            if (customerName == null || customerName.isEmpty()) {
                return ResponseEntity.badRequest().body("Customer name cannot be null or empty.");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stockTransactionList = (List<Map<String, Object>>) requestBody.get("stockTransactions");
            if (stockTransactionList == null || stockTransactionList.isEmpty()) {
                return ResponseEntity.badRequest().body("Stock transactions cannot be null or empty.");
            }

            // Step 2: Map and validate stock transactions
            List<StockTransaction> stockTransactions = stockTransactionList.stream()
                .map(this::mapToStockTransaction)
                .toList();

            // Step 3: Generate the invoice
            LocalDateTime dateOfSale = LocalDateTime.now();
            Invoice invoice = invoiceService.generateInvoiceAndRecordTransaction(customerName, stockTransactions, dateOfSale);

            return ResponseEntity.ok(invoice);

        } catch (IllegalArgumentException e) {
            // Handle specific validation errors
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Handle general errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("An error occurred while generating the invoice: " + e.getMessage());
        }
    }

    // Helper method to map and validate a stock transaction
    private StockTransaction mapToStockTransaction(Map<String, Object> stockTransactionMap) {
        try {
            // Step 1: Validate product
            @SuppressWarnings("unchecked")
            Map<String, Object> productMap = (Map<String, Object>) stockTransactionMap.get("product");
            if (productMap == null || !productMap.containsKey("id")) {
                throw new IllegalArgumentException("Each stock transaction must include a valid product ID.");
            }
            
            
            
//            Product product = new Product();
//            product.setId(((Number) productMap.get("id")).longValue());
//            
            //debugging only, uncomment the top later
            Long productId = ((Number) productMap.get("id")).longValue();
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
            
            // Step 2: Validate quantity
            if (!stockTransactionMap.containsKey("quantity")) {
                throw new IllegalArgumentException("Each stock transaction must include a 'quantity'.");
            }
            int quantity = ((Number) stockTransactionMap.get("quantity")).intValue();
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero.");
            }

            // Step 3: Validate price per unit
            if (!stockTransactionMap.containsKey("pricePerUnit")) {
                throw new IllegalArgumentException("Each stock transaction must include a 'pricePerUnit'.");
            }
            BigDecimal pricePerUnit = BigDecimal.valueOf(((Number) stockTransactionMap.get("pricePerUnit")).doubleValue());

            // Step 4: Create and return the StockTransaction
            StockTransaction stockTransaction = new StockTransaction();
            stockTransaction.setProduct(product);
            stockTransaction.setQuantity(quantity);
            stockTransaction.setPricePerUnit(pricePerUnit);
            stockTransaction.setTimestamp(LocalDateTime.now());

            return stockTransaction;

        } catch (Exception e) {
            e.printStackTrace();  // Add this for debugging
            throw new IllegalArgumentException("Invalid stock transaction data: " + e.getMessage());
        }
    }

}