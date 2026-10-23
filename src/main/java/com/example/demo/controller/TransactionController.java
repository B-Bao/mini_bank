package com.example.demo.controller;

import com.example.demo.dto.transaction.DepositRequestDTO;
import com.example.demo.entity.Transaction;
import com.example.demo.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(@Valid @RequestBody DepositRequestDTO request) {
        Transaction savedTransaction = transactionService.processDeposit(request);
        return ResponseEntity.ok(savedTransaction);
    }
}