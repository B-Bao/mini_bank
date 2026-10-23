package com.example.demo.dto.tranfer;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransferRequest {

  @NotBlank(message = "Mã tài khoản nguồn không được để trống")
  private String sourceAccountNumber;

  @NotBlank(message = "Mã tài khoản đích không được để trống")
  private String destinationAccountNumber;

  @NotNull(message = "Số tiền chuyển không được để trống")
  @DecimalMin(value = "0.0001", message = "Số tiền chuyển phải lớn hơn 0")
  private BigDecimal amount;

  @NotNull(message = "Phí giao dịch không được để trống")
  @DecimalMin(value = "0.0000", message = "Phí giao dịch không được âm")
  private BigDecimal fee = BigDecimal.ZERO;

  @NotBlank(message = "Đơn vị tiền tệ không được để trống")
  @Size(min = 3, max = 3, message = "Đơn vị tiền tệ phải có 3 ký tự (VD: VND)")
  private String currency = "VND";

  private String description;
}
