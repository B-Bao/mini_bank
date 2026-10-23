package com.example.demo.controller;

import com.example.demo.entity.LedgerEntry;
import com.example.demo.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/{account_id}/ledger-entries")
    public ResponseEntity<Page<LedgerEntry>> getAccountLedgerEntries(
            @PathVariable("account_id") Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        // Chỉ lấy username từ token rồi ném thẳng xuống Service xử lý toàn bộ logic & chống IDOR
        String currentUsername = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<LedgerEntry> entries = ledgerService.getLedgerEntriesByAccount(accountId, currentUsername, pageable);

        return ResponseEntity.ok(entries);
    }
}