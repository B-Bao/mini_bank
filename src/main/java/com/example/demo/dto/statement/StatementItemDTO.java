package com.example.demo.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementItemDTO {
    private String referenceCode;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currency;
    private BigDecimal balanceAfter;
    private String counterAccount;
    private String description;
    private LocalDateTime createdAt;
}
