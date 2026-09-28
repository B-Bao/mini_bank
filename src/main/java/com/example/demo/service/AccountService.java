package com.example.demo.service;

import com.example.demo.dto.account.AccountResponse;
import com.example.demo.dto.account.CreateAccountRequest;
import com.example.demo.entity.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;


@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        // thực hiện xác thực các user_id và loại tiền tệ1`34poiuy',
            // 1. Kiểm tra user_id có tồn tại trong hệ thống hay không
        boolean userExists = userRepository.existsById(request.getUserId());
        if (!userExists) {
            throw new RuntimeException("Lỗi: User ID " + request.getUserId() + " không tồn tại!");
        }

            // 2. Kiểm tra loại tiền tệ có được hỗ trợ không (Ví dụ hệ thống chỉ nhận VND hoặc USD)
        String currency = request.getCurrency().toUpperCase();
        if (!currency.equals("VND") && !currency.equals("USD")) {
            throw new RuntimeException("Lỗi: Loại tiền tệ '" + request.getCurrency() + "' không được hỗ trợ!");
        }

        // thực hiện tạo tài khoản
            // 1. Sinh số tài khoản độc đảm bảo duy nhất
        String accountNumber;
        int maxAttempts = 5; // Giới hạn số lần thử để tránh lặp vô hạn
        int attempt = 0;
        boolean isUnique = false;

        do {
            // Cấu trúc: 4 số đầu mã BIN ngân hàng (9704) + 6 số ngẫu nhiên tiếp theo (Tổng 10 số)
            // Hoặc bạn có thể tăng độ dài lên tùy ý (ví dụ: 9704 + 8 số ngẫu nhiên)
            long randomDigits = 100000L + secureRandom.nextLong(900000L);
            accountNumber = "9704" + randomDigits;

            // Kiểm tra xem số tài khoản này đã tồn tại trong Database chưa
            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                isUnique = true; // Nếu chưa tồn tại -> Thỏa mãn duy nhất, thoát vòng lặp
            }

            attempt++;
            if (attempt >= maxAttempts && !isUnique) {
                throw new RuntimeException("Hệ thống đang quá tải, không thể cấp phát số tài khoản duy nhất lúc này. Vui lòng thử lại!");
            }
        } while (!isUnique);

            // 2. Khởi tạo thực thể Account (Domain Model)
        Account account = new Account();
        account.setUserId(request.getUserId());
        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(request.getCurrency() != null ? request.getCurrency() : "VND");
        account.setStatus("ACTIVE");

            // 3. Persist vào Database
        Account savedAccount = accountRepository.save(account);

            // 4. Map sang Response DTO và trả về
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
}