package com.example.demo.service;

import com.example.demo.dto.transfer.TransferRequest;
import com.example.demo.dto.transfer.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferExecutionService transferExecutionService;

    public TransferResponse executeTransfer(TransferRequest request, String currentUsername) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return transferExecutionService.executeTransferInternal(request, currentUsername);
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == maxRetries) {
                    throw new RuntimeException(
                            "Giao dịch thất bại do xung đột đồng thời quá lớn, vui lòng thử lại sau.", e);
                }
                try {
                    Thread.sleep(50 * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new RuntimeException("Không thể hoàn thành giao dịch chuyển khoản.");
    }
}