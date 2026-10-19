package com.example.demo.service;

import com.example.demo.domain.Money;
import com.example.demo.dto.account.AccountResponse;
import com.example.demo.dto.account.CreateAccountRequest;
import com.example.demo.entity.Account;
import com.example.demo.entity.User;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    // Thực hiện mở tài khoản ngân hàng mới cho user
    @Transactional
    public AccountResponse createAccount(String currentUsername, CreateAccountRequest request) {

        // 1. Tìm thông tin user trong database dựa vào username lấy từ token bảo mật
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản đăng nhập!"));

        // 2. KIỂM TRA PHÂN QUYỀN: Đảm bảo user chỉ được mở tài khoản cho chính mình
        if (!currentUser.getId().equals(request.getUserId())) {
            throw new RuntimeException("Lỗi bảo mật: Bạn chỉ được phép mở tài khoản ngân hàng cho chính mình!");
        }

        // 3. Kiểm tra user_id có tồn tại trong hệ thống hay không
        boolean userExists = userRepository.existsById(request.getUserId());
        if (!userExists) {
            throw new RuntimeException("Lỗi: User ID " + request.getUserId() + " không tồn tại!");
        }

        // 4. Kiểm tra loại tiền tệ có được hỗ trợ không
        String rawCurrency = request.getCurrency();
        if (rawCurrency == null || rawCurrency.trim().isEmpty()) {
            throw new RuntimeException("Lỗi: Loại tiền tệ không được để trống!");
        }
        String currency = rawCurrency.toUpperCase();
        if (!currency.equals("VND") && !currency.equals("USD")) {
            throw new RuntimeException("Lỗi: Loại tiền tệ '" + rawCurrency + "' không được hỗ trợ!");
        }

        // 5. Sinh số tài khoản đảm bảo duy nhất
        String accountNumber;
        int maxAttempts = 5;
        int attempt = 0;
        boolean isUnique = false;

        do {
            long randomDigits = 100000L + secureRandom.nextLong(900000L);
            accountNumber = "9704" + randomDigits;

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                isUnique = true;
            }

            attempt++;
            if (attempt >= maxAttempts && !isUnique) {
                throw new RuntimeException("Hệ thống đang quá tải, không thể cấp phát số tài khoản duy nhất lúc này. Vui lòng thử lại!");
            }
        } while (!isUnique);

        // 6. Khởi tạo số dư ban đầu thông qua Value Object Money (tự động chuẩn hóa scale: VND=0, USD=2)
        Money initialMoney = new Money(BigDecimal.ZERO, currency);

        // 7. Khởi tạo thực thể Account (Domain Model)
        Account account = new Account();
        account.setUserId(request.getUserId());
        account.setAccountNumber(accountNumber);
        account.setBalance(initialMoney.getAmount());
        account.setCurrency(initialMoney.getCurrency());
        account.setStatus("ACTIVE");

        // 8. Persist vào Database
        Account savedAccount = accountRepository.save(account);

        // 9. Map sang Response DTO và trả về
        return AccountResponse.builder()
                .accountsId(savedAccount.getAccountsId())
                .userId(savedAccount.getUserId())
                .accountNumber(savedAccount.getAccountNumber())
                .balance(savedAccount.getBalance())
                .currency(savedAccount.getCurrency())
                .status(savedAccount.getStatus())
                .createdAt(savedAccount.getCreatedAt())
                .build();
    }

    // Lấy toàn bộ danh sách các tài khoản của user đang đăng nhập
    public List<AccountResponse> getMyAccounts(String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản đăng nhập!"));

        List<Account> accounts = accountRepository.findByUserId(currentUser.getId());

        return accounts.stream().map(account -> AccountResponse.builder()
                .accountsId(account.getAccountsId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    // Xem số dư/chi tiết của 1 tài khoản cụ thể (Có chống IDOR)
    public AccountResponse getAccountBalance(String currentUsername, Long accountId) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin tài khoản đăng nhập!"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản ngân hàng với ID: " + accountId));

        // KIỂM TRA BẢO MẬT IDOR
        if (!account.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Lỗi bảo mật: Bạn không có quyền xem thông tin tài khoản của người khác!");
        }

        return AccountResponse.builder()
                .accountsId(account.getAccountsId())
                .userId(account.getUserId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }
}