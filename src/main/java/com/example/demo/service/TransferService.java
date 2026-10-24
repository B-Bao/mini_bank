package com.example.demo.service;

import com.example.demo.dto.transfer.TransferRequest;
import com.example.demo.entity.Account;
import com.example.demo.entity.Transfer;
import com.example.demo.exception.custom.AccountNotActiveException;
import com.example.demo.exception.custom.BadRequestException;
import com.example.demo.exception.custom.ResourceNotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService {

  private final AccountRepository accountRepository;
  private final TransferRepository transferRepository;
  private final LedgerService ledgerService;

  // Cơ chế retry thủ công tối ưu cho Optimistic Locking (Không cần spring-retry)
  public Transfer executeTransfer(TransferRequest request) {
    int maxRetries = 3;
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        return executeTransferInternal(request);
      } catch (ObjectOptimisticLockingFailureException e) {
        if (attempt == maxRetries) {
          throw new RuntimeException(
              "Giao dịch thất bại do xung đột đồng thời quá lớn, vui lòng thử lại sau.", e);
        }
        try {
          Thread.sleep(50 * attempt); // Chờ ngắn trước khi thử lại
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      }
    }
    throw new RuntimeException("Không thể hoàn thành giao dịch chuyển khoản.");
  }

  @Transactional
  protected Transfer executeTransferInternal(TransferRequest request) {
    // 1. Kiểm tra tài khoản nguồn và đích không được trùng nhau
    if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
      throw new BadRequestException("Tài khoản nguồn và tài khoản đích phải khác nhau");
    }

    // 2. Tìm kiếm 2 tài khoản
    Account sourceAccount =
        accountRepository
            .findByAccountNumber(request.getSourceAccountNumber())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy tài khoản nguồn: " + request.getSourceAccountNumber()));

    Account destinationAccount =
        accountRepository
            .findByAccountNumber(request.getDestinationAccountNumber())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy tài khoản đích: " + request.getDestinationAccountNumber()));

    // 3. Kiểm tra trạng thái tài khoản
    if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())
        || !"ACTIVE".equalsIgnoreCase(destinationAccount.getStatus())) {
      throw new AccountNotActiveException(
          "Một trong hai tài khoản đang không ở trạng thái hoạt động");
    }

    // 4. Kiểm tra số dư tài khoản nguồn (Amount + Fee)
    BigDecimal totalDebit = request.getAmount().add(request.getFee());
    if (sourceAccount.getBalance().compareTo(totalDebit) < 0) {
      throw new BadRequestException("Số dư tài khoản nguồn không đủ để thực hiện giao dịch");
    }

    // 5. Thực hiện trừ tiền nguồn, cộng tiền đích
    sourceAccount.setBalance(sourceAccount.getBalance().subtract(totalDebit));
    destinationAccount.setBalance(destinationAccount.getBalance().add(request.getAmount()));

    // 6. Lưu thay đổi (Optimistic Lock tự động kiểm tra cột @Version)
    accountRepository.save(sourceAccount);
    accountRepository.save(destinationAccount);

    // 7. Tạo mã giao dịch chung
    String referenceCode =
        "TRF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

    // 8. Lưu lịch sử chuyển khoản vào bảng transfers
    Transfer transfer =
        Transfer.builder()
            .referenceCode(referenceCode)
            .sourceAccountId(sourceAccount.getAccountsId())
            .destinationAccountId(destinationAccount.getAccountsId())
            .amount(request.getAmount())
            .fee(request.getFee())
            .currency(request.getCurrency())
            .status("SUCCESS")
            .description(request.getDescription())
            .build();

    Transfer savedTransfer = transferRepository.save(transfer);

    // 9. Ghi sổ cái kép (Ledger Entries)
    ledgerService.recordTransferEntry(
        referenceCode,
        sourceAccount,
        destinationAccount,
        request.getAmount(),
        request.getFee(),
        request.getCurrency(),
        request.getDescription());

    return savedTransfer;
  }
}
