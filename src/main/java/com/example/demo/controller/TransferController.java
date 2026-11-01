package com.example.demo.controller;

import com.example.demo.dto.transfer.TransferRequest;
import com.example.demo.dto.transfer.TransferResponse;
import com.example.demo.service.TransferService;
import jakarta.validation.Valid;
import java.security.Principal;
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
  public ResponseEntity<TransferResponse> transferMoney(
      @Valid @RequestBody TransferRequest request, Principal principal) {
    String currentUsername = principal.getName();
    TransferResponse response = transferService.executeTransfer(request, currentUsername);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
