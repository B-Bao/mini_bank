package com.example.demo.controller;

import com.example.demo.dto.statement.PagedResponseDTO;
import com.example.demo.dto.statement.StatementItemDTO;
import com.example.demo.service.AccountStatementService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Validated
public class AccountStatementController {

  private final AccountStatementService statementService;

  @GetMapping("/{accountId}/statements")
  public ResponseEntity<PagedResponseDTO<StatementItemDTO>> getAccountStatements(
      @PathVariable Long accountId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0")
          @Min(value = 0, message = "Số trang (page) không được nhỏ hơn 0")
          int page,
      @RequestParam(defaultValue = "10")
          @Min(value = 1, message = "Kích thước trang (size) phải lớn hơn hoặc bằng 1")
          @Max(value = 100, message = "Kích thước trang (size) tối đa không được vượt quá 100")
          int size) {
    // Gọi Service xử lý bảo mật IDOR, lọc dữ liệu sổ cái và phân trang
    PagedResponseDTO<StatementItemDTO> response =
        statementService.getStatements(accountId, fromDate, toDate, type, page, size);

    return ResponseEntity.ok(response);
  }
}
