package com.example.demo.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class DepositRequestDTO {
  @NotBlank(message = "Mã tham chiếu (referenceNumber) không được để trống")
  private String referenceNumber;

  @NotBlank(message = "Số tài khoản nhận không được để trống")
  private String receiverAccountNumber;

  @NotBlank(message = "Tên chủ tài khoản nhận không được để trống")
  private String receiverName;

  @NotNull(message = "Số tiền nạp không được để trống")
  @DecimalMin(value = "0.01", message = "Số tiền nạp phải lớn hơn 0")
  private BigDecimal amount;

  private String description;
}
