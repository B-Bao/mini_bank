package com.example.demo.controller;

import com.example.demo.dto.statement.PagedResponseDTO;
import com.example.demo.dto.statement.StatementItemDTO;
import com.example.demo.service.AccountStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountStatementController {

    private final AccountStatementService statementService;

    @GetMapping("/{accountId}/statements")
    public ResponseEntity<PagedResponseDTO<StatementItemDTO>> getAccountStatements(
            @PathVariable Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Gọi Service xử lý bảo mật IDOR, lọc dữ liệu sổ cái và phân trang
        PagedResponseDTO<StatementItemDTO> response = statementService.getStatements(
                accountId, fromDate, toDate, type, page, size
        );

        return ResponseEntity.ok(response);
    }
}