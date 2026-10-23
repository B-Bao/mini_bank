package com.example.demo.service;

import com.example.demo.dto.transaction.DepositRequestDTO;
import com.example.demo.entity.Account;
import com.example.demo.entity.LedgerEntry;
import com.example.demo.entity.Transaction;
import com.example.demo.exception.custom.AccountNotActiveException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;
  private final LedgerService ledgerService;

  // Constructor Injection chuẩn Best Practice
  public TransactionService(
      TransactionRepository transactionRepository,
      AccountRepository accountRepository,
      UserRepository userRepository,
      LedgerService ledgerService) {
    this.transactionRepository = transactionRepository;
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
    this.ledgerService = ledgerService;
  }

  @Transactional(rollbackFor = Exception.class)
  public Transaction processDeposit(DepositRequestDTO request) {
    // 1. Kiểm tra Idempotency (Chống trùng lặp request nạp tiền)
    if (transactionRepository.existsByReferenceNumber(request.getReferenceNumber())) {
      throw new IllegalArgumentException(
          "Giao dịch với mã tham chiếu này đã tồn tại: " + request.getReferenceNumber());
    }

    // 2. Lấy username/email từ Token (Security Context)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Bạn chưa đăng nhập hoặc Token không hợp lệ!");
    }
    String currentUsername = authentication.getName();

    // 3. Lấy thông tin user hiện tại từ DB để suy ra userId
    Long currentUserId =
        userRepository
            .findByUsername(currentUsername)
            .map(user -> user.getId())
            .orElseThrow(
                () -> new RuntimeException("Không tìm thấy thông tin người dùng trong hệ thống"));

    // 4. Tìm tài khoản nhận tiền trong Database theo số tài khoản
    Account receiverAccount =
        accountRepository
            .findByAccountNumber(request.getReceiverAccountNumber())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Không tìm thấy số tài khoản nhận: " + request.getReceiverAccountNumber()));

    // 5. KIỂM TRA TRẠNG THÁI TÀI KHOẢN (ACTIVE)
    if (!"ACTIVE".equalsIgnoreCase(receiverAccount.getStatus())) {
      throw new AccountNotActiveException(
          "Giao dịch thất bại: Tài khoản nhận tiền hiện đang không hoạt động (Trạng thái: "
              + receiverAccount.getStatus()
              + ")");
    }

    // 6. XÁC THỰC CHỐNG IDOR:
    // So sánh userId của chủ tài khoản với userId của người đang cầm token gọi API
    if (!receiverAccount.getUserId().equals(currentUserId)) {
      throw new AccessDeniedException(
          "Cảnh báo bảo mật: Bạn không có quyền nạp tiền vào số tài khoản không thuộc sở hữu của mình!");
    }

    // 7. Khởi tạo đối tượng Transaction chuẩn core banking
    Transaction tx = new Transaction();
    tx.setReferenceNumber(request.getReferenceNumber());
    tx.setTraceNumber("TRC" + System.currentTimeMillis());

    tx.setSenderAccountNumber("SYSTEM_GATEWAY");
    tx.setSenderName("Payment Gateway VNPay/MoMo");

    tx.setReceiverAccountNumber(receiverAccount.getAccountNumber());
    tx.setReceiverName(request.getReceiverName());
    tx.setBeneficiaryBankCode("INTERNAL");

    tx.setAmount(request.getAmount());
    tx.setFeeAmount(BigDecimal.ZERO);
    tx.setVatAmount(BigDecimal.ZERO);
    tx.setCurrencyCode("VND");
    tx.setFeePayer("RECEIVER");

    tx.setTransactionType("DEPOSIT");
    tx.setChannel("API");
    tx.setStatus("SUCCESS");

    tx.setDescription(
        request.getDescription() != null ? request.getDescription() : "Nạp tiền vào tài khoản");
    tx.setValueDate(LocalDate.now());
    tx.setCompletedAt(OffsetDateTime.now());

    // Lưu transaction trước để sinh ra transaction_id (UUID)
    Transaction savedTx = transactionRepository.save(tx);

    // 8. Lưu vết số dư trước và sau khi cộng tiền (Phục vụ đối soát kế toán)
    BigDecimal balanceBefore = receiverAccount.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(request.getAmount());

    receiverAccount.setBalance(balanceAfter);
    accountRepository.save(receiverAccount);

    // 9. GHI NHẬN SỔ CÁI (LEDGER ENTRY) - Bút toán CREDIT (Ghi Có) tăng tiền tài khoản khách hàng
    ledgerService.recordEntry(
        savedTx.getTransactionId(),
        receiverAccount.getAccountsId(),
        LedgerEntry.EntryType.CREDIT,
        request.getAmount(),
        balanceBefore,
        balanceAfter,
        "Nạp tiền thành công qua cổng thanh toán - Ref: " + request.getReferenceNumber());

    // 10. Trả về thông tin giao dịch đã lưu
    return savedTx;
  }
}
