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
   * Ghi nhận bút toán sổ cái một cách bất biến (Immutable) Yêu cầu bắt buộc phải chạy trong một
   * Transaction hiện hành (Propagation.MANDATORY)
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

  /** Truy vấn lịch sử bút toán của tài khoản có phân trang (Kèm xác thực chống IDOR) */
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
}
