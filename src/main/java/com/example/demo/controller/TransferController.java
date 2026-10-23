package com.example.demo.controller;

import com.example.demo.dto.transfer.TransferRequest;
import com.example.demo.entity.Transfer;
import com.example.demo.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Transfer> transferMoney(@Valid @RequestBody TransferRequest request) {
        Transfer transfer = transferService.executeTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }
}