package com.example.demo.service;

import com.example.demo.dto.statement.PagedResponseDTO;
import com.example.demo.dto.statement.StatementItemDTO;
import com.example.demo.entity.Account;
import com.example.demo.entity.LedgerEntry;
import com.example.demo.entity.User;
import com.example.demo.exception.custom.ResourceNotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.LedgerEntryRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountStatementService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public PagedResponseDTO<StatementItemDTO> getStatements(
            Long accountId, LocalDate fromDate, LocalDate toDate, String typeStr, int page, int size
    ) {
        // thực hiện cơ chế xác thực IDOR
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại: " + currentUsername));
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với ID: " + accountId));
        if (!account.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập vào sao kê của tài khoản này!");
        }

        // thực hiện xử lý lọc và phân trang
        OffsetDateTime startDateTime = (fromDate != null) ? fromDate.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime endDateTime = (toDate != null) ? toDate.atTime(23, 59, 59).atOffset(ZoneOffset.UTC) : null;

        LedgerEntry.EntryType entryType = null;
        if (typeStr != null && !typeStr.trim().isEmpty()) {
            try {
                entryType = LedgerEntry.EntryType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<LedgerEntry> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("accountId"), accountId);
        if (startDateTime != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDateTime));
        }
        if (endDateTime != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDateTime));
        }
        if (entryType != null) {
            LedgerEntry.EntryType requestedEntryType = entryType;
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("entryType"), requestedEntryType));
        }

        Page<LedgerEntry> ledgerPage = ledgerEntryRepository.findAll(specification, pageable);

        List<StatementItemDTO> items = ledgerPage.getContent().stream()
                .map(ledger -> StatementItemDTO.builder()
                        .referenceCode(ledger.getTransactionId() != null ? ledger.getTransactionId().toString() : null)
                        .transactionType(ledger.getEntryType() != null ? ledger.getEntryType().name() : null)
                        .amount(ledger.getAmount())
                        .fee(java.math.BigDecimal.ZERO)
                        .currency("VND")
                        .balanceAfter(ledger.getBalanceAfter())
                        .description(ledger.getDescription())
                        .createdAt(ledger.getCreatedAt() != null ? ledger.getCreatedAt().toLocalDateTime() : null)
                        .build())
                .collect(Collectors.toList());

        return PagedResponseDTO.<StatementItemDTO>builder()
                .content(items)
                .pageNumber(ledgerPage.getNumber())
                .pageSize(ledgerPage.getSize())
                .totalElements(ledgerPage.getTotalElements())
                .totalPages(ledgerPage.getTotalPages())
                .last(ledgerPage.isLast())
                .build();
    }
}