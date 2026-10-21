package com.example.demo.dto.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

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
