package com.example.demo.service;

import com.example.demo.dto.transaction.DepositRequestDTO;
import com.example.demo.entity.Account;
import com.example.demo.entity.Transaction;
import com.example.demo.exception.custom.AccountNotActiveException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    // Constructor Injection (Sạch warning, chuẩn best practice)
    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction processDeposit(DepositRequestDTO request) {
        // 1. Kiểm tra Idempotency (Chống trùng lặp request nạp tiền)
        if (transactionRepository.existsByReferenceNumber(request.getReferenceNumber())) {
            throw new IllegalArgumentException("Giao dịch với mã tham chiếu này đã tồn tại: " + request.getReferenceNumber());
        }

        // 2. Lấy username/email từ Token (Security Context)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Bạn chưa đăng nhập hoặc Token không hợp lệ!");
        }
        String currentUsername = authentication.getName();

        // 3. Lấy thông tin user hiện tại từ DB để suy ra userId
        Long currentUserId = userRepository.findByUsername(currentUsername)
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng trong hệ thống"));

        // 4. Tìm tài khoản nhận tiền trong Database theo số tài khoản
        Account receiverAccount = accountRepository.findByAccountNumber(request.getReceiverAccountNumber())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy số tài khoản nhận: " + request.getReceiverAccountNumber()));

        // 5. KIỂM TRA TRẠNG THÁI TÀI KHOẢN (ACTIVE)
        if (!"ACTIVE".equalsIgnoreCase(receiverAccount.getStatus())) {
            throw new AccountNotActiveException("Giao dịch thất bại: Tài khoản nhận tiền hiện đang không hoạt động (Trạng thái: " + receiverAccount.getStatus() + ")");
        }

        // 6. XÁC THỰC CHỐNG IDOR:
        // So sánh userId của chủ tài khoản với userId của người đang cầm token gọi API
        if (!receiverAccount.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("Cảnh báo bảo mật: Bạn không có quyền nạp tiền vào số tài khoản không thuộc sở hữu của mình!");
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

        tx.setDescription(request.getDescription() != null ? request.getDescription() : "Nạp tiền vào tài khoản");
        tx.setValueDate(LocalDate.now());
        tx.setCompletedAt(OffsetDateTime.now());

        // 8. Cập nhật cộng tiền vào số dư tài khoản
        receiverAccount.setBalance(receiverAccount.getBalance().add(request.getAmount()));
        accountRepository.save(receiverAccount);

        // 9. Lưu lịch sử giao dịch xuống database và trả về
        return transactionRepository.save(tx);
    }
}