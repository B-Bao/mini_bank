package com.example.demo.controller;

import com.example.demo.dto.account.AccountResponse;
import com.example.demo.dto.account.CreateAccountRequest;
import com.example.demo.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // API 1: Tạo tài khoản mới (POST)
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Principal principal
    ) {
        String currentUsername = principal.getName();
        AccountResponse response = accountService.createAccount(currentUsername, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // API 2: Lấy danh sách tất cả tài khoản của tôi (GET)
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Principal principal) {
        String currentUsername = principal.getName();
        List<AccountResponse> accounts = accountService.getMyAccounts(currentUsername);
        return ResponseEntity.ok(accounts);
    }

    // API 3: Xem số dư chi tiết của một tài khoản theo ID (GET)
    @GetMapping("/{account_id}/balance")
    public ResponseEntity<AccountResponse> getAccountBalance(
            @PathVariable("account_id") Long accountId,
            Principal principal
    ) {
        String currentUsername = principal.getName();
        AccountResponse response = accountService.getAccountBalance(currentUsername, accountId);
        return ResponseEntity.ok(response);
    }
}