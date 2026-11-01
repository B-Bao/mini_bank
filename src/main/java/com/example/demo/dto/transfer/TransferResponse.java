package com.example.demo.dto.transfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferResponse {
  private Long id;
  private String referenceCode;
  private String sourceAccountNumber;
  private String destinationAccountNumber;
  private BigDecimal amount;
  private BigDecimal fee;
  private String currency;
  private String status;
  private String description;
  private LocalDateTime createdAt;
}
