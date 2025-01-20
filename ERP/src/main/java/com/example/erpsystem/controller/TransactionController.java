package com.example.erpsystem.controller;

import com.example.erpsystem.model.Transaction;
import com.example.erpsystem.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/accounting/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<Transaction> recordTransaction(@RequestBody Transaction transactionRequest) {
        Transaction transaction = transactionService.recordTransaction(
                transactionRequest.getDescription(),
                transactionRequest.getAmount(),
                transactionRequest.getType(),
                transactionRequest.getInvoice()
        );
        return ResponseEntity.ok(transaction);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/bill/{transactionId}")
    public ResponseEntity<String> generateBill(@PathVariable Long transactionId) throws IOException {
        String message = transactionService.generateBill(transactionId);
        return ResponseEntity.ok(message);
    }
}
