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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService {

  private final AccountRepository accountRepository;
  private final TransferRepository transferRepository;
  private final LedgerService ledgerService; // Inject LedgerService để ghi bút toán sổ cái

  @Transactional
  public Transfer executeTransfer(TransferRequest request) {
    // 1. Kiểm tra tài khoản nguồn và đích không được trùng nhau
    if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
      throw new BadRequestException("Tài khoản nguồn và tài khoản đích phải khác nhau");
    }

    // 2. Lấy thông tin tài khoản nguồn (Khóa bi quan chống race condition)
    Account sourceAccount =
        accountRepository
            .findByAccountNumberWithLock(request.getSourceAccountNumber())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy tài khoản nguồn: " + request.getSourceAccountNumber()));

    // 3. Lấy thông tin tài khoản đích
    Account destinationAccount =
        accountRepository
            .findByAccountNumberWithLock(request.getDestinationAccountNumber())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Không tìm thấy tài khoản đích: " + request.getDestinationAccountNumber()));

    // 4. Kiểm tra trạng thái tài khoản
    if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())
        || !"ACTIVE".equalsIgnoreCase(destinationAccount.getStatus())) {
      throw new AccountNotActiveException(
          "Một trong hai tài khoản đang không ở trạng thái hoạt động");
    }

    // 5. Tính tổng số tiền trừ (Amount + Fee)
    BigDecimal totalDebit = request.getAmount().add(request.getFee());
    if (sourceAccount.getBalance().compareTo(totalDebit) < 0) {
      throw new BadRequestException("Số dư tài khoản nguồn không đủ để thực hiện giao dịch");
    }

    // 6. Thực hiện trừ tiền nguồn và cộng tiền đích
    sourceAccount.setBalance(sourceAccount.getBalance().subtract(totalDebit));
    destinationAccount.setBalance(destinationAccount.getBalance().add(request.getAmount()));

    accountRepository.save(sourceAccount);
    accountRepository.save(destinationAccount);

    // 7. Tạo mã giao dịch chung (Reference Code)
    String referenceCode =
        "TRF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

    // 8. Lưu lịch sử giao dịch chuyển khoản vào bảng transfers
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

    // 9. Ghi sổ cái (Ledger Entries) cho giao dịch chuyển khoản
    // Tùy theo cách thiết kế của LedgerService trong project của bạn, bạn có thể gọi phương thức
    // ghi sổ cái tương ứng:
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
