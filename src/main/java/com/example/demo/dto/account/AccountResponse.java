package com.example.demo.dto.account;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {
    private Long accountsId;
    private Long userId;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}