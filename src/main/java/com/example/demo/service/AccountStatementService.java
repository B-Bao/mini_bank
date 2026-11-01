package com.example.demo.service;

import com.example.demo.dto.statement.PagedResponseDTO;
import com.example.demo.dto.statement.StatementItemDTO;
import com.example.demo.entity.Account;
import com.example.demo.entity.LedgerEntry;
import com.example.demo.entity.User;
import com.example.demo.exception.custom.BadRequestException;
import com.example.demo.exception.custom.ResourceNotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.LedgerEntryRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountStatementService {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  public PagedResponseDTO<StatementItemDTO> getStatements(
      Long accountId, LocalDate fromDate, LocalDate toDate, String typeStr, int page, int size) {
    // 1. Xác thực an toàn tránh NullPointerException khi lấy Authentication
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Người dùng chưa được xác thực!");
    }
    String currentUsername = authentication.getName();

    User currentUser =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy người dùng hiện tại: " + currentUsername));

    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Không tìm thấy tài khoản với ID: " + accountId));

    // Kiểm tra quyền sở hữu (Chống IDOR)
    if (!account.getUserId().equals(currentUser.getId())) {
      throw new AccessDeniedException("Bạn không có quyền truy cập vào sao kê của tài khoản này!");
    }

    // 2. Validate khoảng thời gian
    if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
      throw new BadRequestException(
          "Khoảng thời gian không hợp lệ: 'fromDate' phải nhỏ hơn hoặc bằng 'toDate'.");
    }

    // Đưa vào biến final để sử dụng an toàn trong Lambda expression
    final LocalDateTime finalStartDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
    final LocalDateTime finalEndDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

    // 3. Xử lý loại giao dịch (EntryType)
    LedgerEntry.EntryType parsedEntryType = null;
    if (typeStr != null && !typeStr.trim().isEmpty()) {
      try {
        parsedEntryType = LedgerEntry.EntryType.valueOf(typeStr.toUpperCase());
      } catch (IllegalArgumentException ignored) {
      }
    }
    final LedgerEntry.EntryType finalEntryType = parsedEntryType;

    // 4. Xây dựng Specification
    Specification<LedgerEntry> spec =
        (root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new ArrayList<>();

          predicates.add(criteriaBuilder.equal(root.get("accountId"), accountId));

          if (finalStartDateTime != null) {
            predicates.add(
                criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), finalStartDateTime));
          }
          if (finalEndDateTime != null) {
            predicates.add(
                criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), finalEndDateTime));
          }
          if (finalEntryType != null) {
            predicates.add(criteriaBuilder.equal(root.get("entryType"), finalEntryType));
          }

          return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<LedgerEntry> ledgerPage = ledgerEntryRepository.findAll(spec, pageable);

    // 5. Map sang DTO
    List<StatementItemDTO> items =
        ledgerPage.getContent().stream()
            .map(
                ledger -> {
                  String refCode =
                      ledger.getTransactionId() != null
                          ? ledger.getTransactionId().toString()
                          : null;

                  if (ledger.getDescription() != null
                      && ledger.getDescription().contains("Ref: ")) {
                    int idx = ledger.getDescription().indexOf("Ref: ");
                    refCode = ledger.getDescription().substring(idx + 5).trim();
                  }

                  return StatementItemDTO.builder()
                      .referenceCode(refCode)
                      .transactionType(
                          ledger.getEntryType() != null ? ledger.getEntryType().name() : null)
                      .amount(ledger.getAmount())
                      .fee(java.math.BigDecimal.ZERO)
                      .currency("VND")
                      .balanceAfter(ledger.getBalanceAfter())
                      .description(ledger.getDescription())
                      .counterAccount(null)
                      .createdAt(
                          ledger.getCreatedAt() != null
                              ? ledger.getCreatedAt().toLocalDateTime()
                              : null)
                      .build();
                })
            .collect(Collectors.toList());

    // Khởi tạo PagedResponseDTO an toàn không bị lỗi generic builder
    PagedResponseDTO<StatementItemDTO> response = new PagedResponseDTO<>();
    response.setContent(items);
    response.setPageNumber(ledgerPage.getNumber());
    response.setPageSize(ledgerPage.getSize());
    response.setTotalElements(ledgerPage.getTotalElements());
    response.setTotalPages(ledgerPage.getTotalPages());
    response.setLast(ledgerPage.isLast());

    return response;
  }
}
