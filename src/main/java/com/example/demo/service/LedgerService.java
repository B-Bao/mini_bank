package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.entity.LedgerEntry;
import com.example.demo.entity.User;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.LedgerEntryRepository;
import com.example.demo.repository.UserRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

  private final LedgerEntryRepository ledgerEntryRepository;
  private final AccountRepository accountRepository;
  private final UserRepository userRepository;

  // Constructor Injection đầy đủ các dependency cần thiết
  public LedgerService(
          LedgerEntryRepository ledgerEntryRepository,
          AccountRepository accountRepository,
          UserRepository userRepository) {
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.accountRepository = accountRepository;
    this.userRepository = userRepository;
  }

  /**
   * Ghi nhận bút toán sổ cái một cách bất biến (Immutable)
   * Yêu cầu bắt buộc phải chạy trong một Transaction hiện hành (Propagation.MANDATORY)
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordEntry(
          UUID transactionId,
          Long accountId,
          LedgerEntry.EntryType entryType,
          BigDecimal amount,
          BigDecimal balanceBefore,
          BigDecimal balanceAfter,
          String description) {

    LedgerEntry entry = new LedgerEntry();
    entry.setTransactionId(transactionId);
    entry.setAccountId(accountId);
    entry.setEntryType(entryType);
    entry.setAmount(amount);
    entry.setBalanceBefore(balanceBefore);
    entry.setBalanceAfter(balanceAfter);
    entry.setDescription(description);
    entry.setCreatedAt(OffsetDateTime.now());

    // Chỉ thực hiện INSERT, đảm bảo tính bất biến của sổ cái kế toán
    ledgerEntryRepository.save(entry);
  }

  // ghi sổ cái ledger chuyên biệt cho transfer, ghi lại nguồn và đích giao dịch (bút toán kép)
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordTransferEntry(
          String referenceCode,
          Account sourceAccount,
          Account destinationAccount,
          BigDecimal amount,
          BigDecimal fee,
          String currency,
          String description) {

    // Chuyển đổi referenceCode thành UUID (hoặc bạn có thể điều chỉnh nếu transactionId trong LedgerEntry là String)
    UUID txnId = UUID.nameUUIDFromBytes(referenceCode.getBytes());

    // 1. Ghi bút toán DEBIT (Ghi giảm) cho tài khoản nguồn (Amount + Fee)
    BigDecimal totalSourceDebit = amount.add(fee);
    BigDecimal sourceBalanceBefore = sourceAccount.getBalance().add(totalSourceDebit); // Khôi phục lại số dư trước giao dịch để ghi log

    LedgerEntry sourceEntry = new LedgerEntry();
    sourceEntry.setTransactionId(txnId);
    sourceEntry.setAccountId(sourceAccount.getAccountsId());
    sourceEntry.setEntryType(LedgerEntry.EntryType.DEBIT);
    sourceEntry.setAmount(totalSourceDebit);
    sourceEntry.setBalanceBefore(sourceBalanceBefore);
    sourceEntry.setBalanceAfter(sourceAccount.getBalance());
    sourceEntry.setDescription("Chuyển tiền đến " + destinationAccount.getAccountNumber() + (description != null ? " - " + description : ""));
    sourceEntry.setCreatedAt(OffsetDateTime.now());

    ledgerEntryRepository.save(sourceEntry);

    // 2. Ghi bút toán CREDIT (Ghi tăng) cho tài khoản đích (Chỉ số tiền gốc)
    BigDecimal destBalanceBefore = destinationAccount.getBalance().subtract(amount); // Khôi phục số dư trước khi cộng

    LedgerEntry destEntry = new LedgerEntry();
    destEntry.setTransactionId(txnId);
    destEntry.setAccountId(destinationAccount.getAccountsId());
    destEntry.setEntryType(LedgerEntry.EntryType.CREDIT);
    destEntry.setAmount(amount);
    destEntry.setBalanceBefore(destBalanceBefore);
    destEntry.setBalanceAfter(destinationAccount.getBalance());
    destEntry.setDescription("Nhận tiền từ " + sourceAccount.getAccountNumber() + (description != null ? " - " + description : ""));
    destEntry.setCreatedAt(OffsetDateTime.now());

    ledgerEntryRepository.save(destEntry);
  }


  //ghi sổ cái cho Transaction (bút toán đơn)
  public Page<LedgerEntry> getLedgerEntriesByAccount(
          Long accountId, String currentUsername, Pageable pageable) {
    // 1. Kiểm tra tài khoản có tồn tại trong hệ thống không
    Account account =
            accountRepository
                    .findById(accountId)
                    .orElseThrow(
                            () ->
                                    new RuntimeException(
                                            "Không tìm thấy tài khoản ngân hàng với ID: " + accountId));

    // 2. Lấy thông tin user hiện tại từ Token
    User currentUser =
            userRepository
                    .findByUsername(currentUsername)
                    .orElseThrow(
                            () -> new RuntimeException("Không tìm thấy thông tin người dùng trong hệ thống!"));

    // 3. KIỂM TRA BẢO MẬT IDOR: Đảm bảo tài khoản thực sự thuộc sở hữu của user đang gọi API
    if (!account.getUserId().equals(currentUser.getId())) {
      throw new AccessDeniedException(
              "Cảnh báo bảo mật: Bạn không có quyền xem lịch sử sổ cái của tài khoản này!");
    }

    // 4. Trả về kết quả phân trang sổ cái theo thời gian mới nhất
    return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
  }
// ghi bút toán điều chỉnh
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordAdjustingEntry(
          UUID originalTransactionId,
          Long accountId,
          LedgerEntry.EntryType entryType, // DEBIT hoặc CREDIT
          BigDecimal adjustmentAmount,
          BigDecimal balanceBefore,
          BigDecimal balanceAfter,
          String reason) {

    LedgerEntry entry = new LedgerEntry();
    entry.setTransactionId(originalTransactionId);
    entry.setAccountId(accountId);
    entry.setEntryType(entryType);
    entry.setAmount(adjustmentAmount);
    entry.setBalanceBefore(balanceBefore);
    entry.setBalanceAfter(balanceAfter);
    entry.setDescription("[ĐIỀU CHỈNH] " + reason);
    entry.setCreatedAt(OffsetDateTime.now());

    ledgerEntryRepository.save(entry);
  }

  // ghi sổ cái cho bút toán đảo
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordReversalEntry(
          UUID originalTransactionId,
          Account sourceAccount,
          Account destinationAccount,
          BigDecimal refundAmount,
          BigDecimal feeToRefund,
          String currency,
          String reason) {

    UUID reversalTxnId = UUID.randomUUID(); // Mã giao dịch đảo mới

    // A. Đảo tài khoản nguồn: Trước đó bị trừ tiền (DEBIT), nay hoàn tiền thì ghi tăng lại (CREDIT)
    BigDecimal sourceBalanceBefore = sourceAccount.getBalance();
    sourceAccount.setBalance(sourceAccount.getBalance().add(refundAmount.add(feeToRefund)));

    LedgerEntry sourceReversalEntry = new LedgerEntry();
    sourceReversalEntry.setTransactionId(reversalTxnId);
    sourceReversalEntry.setAccountId(sourceAccount.getAccountsId());
    sourceReversalEntry.setEntryType(LedgerEntry.EntryType.CREDIT); // Đảo chiều thành CREDIT
    sourceReversalEntry.setAmount(refundAmount.add(feeToRefund));
    sourceReversalEntry.setBalanceBefore(sourceBalanceBefore);
    sourceReversalEntry.setBalanceAfter(sourceAccount.getBalance());
    sourceReversalEntry.setDescription("[HOÀN TIỀN/ĐẢO] Giao dịch gốc: " + originalTransactionId + " - " + reason);
    sourceReversalEntry.setCreatedAt(OffsetDateTime.now());

    ledgerEntryRepository.save(sourceReversalEntry);

    // B. Đảo tài khoản đích: Trước đó được cộng tiền (CREDIT), nay thu hồi lại thì ghi giảm (DEBIT)
    BigDecimal destBalanceBefore = destinationAccount.getBalance();
    destinationAccount.setBalance(destinationAccount.getBalance().subtract(refundAmount));

    LedgerEntry destReversalEntry = new LedgerEntry();
    destReversalEntry.setTransactionId(reversalTxnId);
    destReversalEntry.setAccountId(destinationAccount.getAccountsId());
    destReversalEntry.setEntryType(LedgerEntry.EntryType.DEBIT); // Đảo chiều thành DEBIT
    destReversalEntry.setAmount(refundAmount);
    destReversalEntry.setBalanceBefore(destBalanceBefore);
    destReversalEntry.setBalanceAfter(destinationAccount.getBalance());
    destReversalEntry.setDescription("[THU HỒI/ĐẢO] Giao dịch gốc: " + originalTransactionId + " - " + reason);
    destReversalEntry.setCreatedAt(OffsetDateTime.now());

    ledgerEntryRepository.save(destReversalEntry);
  }
}