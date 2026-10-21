package com.example.demo.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {
  @NotNull(message = "User ID không được để trống")
  private Long userId;

  @NotBlank(message = "Đơn vị tiền tệ không được để trống")
  private String currency;
}
