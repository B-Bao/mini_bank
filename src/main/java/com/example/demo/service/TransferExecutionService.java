package com.example.demo.service;

import com.example.demo.dto.transfer.TransferRequest;
import com.example.demo.dto.transfer.TransferResponse;
import com.example.demo.entity.Account;
import com.example.demo.entity.Transfer;
import com.example.demo.entity.User;
import com.example.demo.exception.custom.AccountNotActiveException;
import com.example.demo.exception.custom.BadRequestException;
import com.example.demo.exception.custom.ResourceNotFoundException;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransferRepository;
import com.example.demo.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferExecutionService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;

    @Transactional(rollbackFor = Exception.class)
    public TransferResponse executeTransferInternal(TransferRequest request, String currentUsername) {
        // 1. Xác thực user hiện tại từ token
        User currentUser =
                userRepository
                        .findByUsername(currentUsername)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng: " + currentUsername));

        // 2. Kiểm tra tài khoản nguồn và đích không được trùng nhau
        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw new BadRequestException("Tài khoản nguồn và tài khoản đích phải khác nhau");
        }

        // 3. Tìm kiếm 2 tài khoản
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

        // 4. BẢO MẬT IDOR: Kiểm tra tài khoản nguồn bắt buộc phải thuộc quyền sở hữu của user đang gọi API
        if (!sourceAccount.getUserId().equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "Cảnh báo bảo mật: Bạn không có quyền thực hiện chuyển tiền từ tài khoản không thuộc sở hữu của mình!");
        }

        // 5. Kiểm tra trạng thái tài khoản
        if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getStatus())
                || !"ACTIVE".equalsIgnoreCase(destinationAccount.getStatus())) {
            throw new AccountNotActiveException(
                    "Một trong hai tài khoản đang không ở trạng thái hoạt động");
        }

        // 6. Kiểm tra số dư tài khoản nguồn (Amount + Fee)
        BigDecimal totalDebit = request.getAmount().add(request.getFee());
        if (sourceAccount.getBalance().compareTo(totalDebit) < 0) {
            throw new BadRequestException("Số dư tài khoản nguồn không đủ để thực hiện giao dịch");
        }

        // 7. Thực hiện trừ tiền nguồn, cộng tiền đích
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(totalDebit));
        destinationAccount.setBalance(destinationAccount.getBalance().add(request.getAmount()));

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        // 8. Tạo mã giao dịch chung
        String referenceCode =
                "TRF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // 9. Lưu lịch sử chuyển khoản
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

        // 10. Ghi sổ cái kép (Ledger Entries)
        ledgerService.recordTransferEntry(
                referenceCode,
                sourceAccount,
                destinationAccount,
                request.getAmount(),
                request.getFee(),
                request.getCurrency(),
                request.getDescription());

        // 11. Map sang Response DTO và trả về
        return TransferResponse.builder()
                .id(savedTransfer.getId())
                .referenceCode(savedTransfer.getReferenceCode())
                .sourceAccountNumber(sourceAccount.getAccountNumber())
                .destinationAccountNumber(destinationAccount.getAccountNumber())
                .amount(savedTransfer.getAmount())
                .fee(savedTransfer.getFee())
                .currency(savedTransfer.getCurrency())
                .status(savedTransfer.getStatus())
                .description(savedTransfer.getDescription())
                .createdAt(savedTransfer.getCreatedAt())
                .build();
    }
}