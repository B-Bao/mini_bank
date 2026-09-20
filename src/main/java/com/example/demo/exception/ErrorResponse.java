package com.example.demo.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
  private int status; // Mã lỗi HTTP (VD: 400, 404, 500)
  private String message; // Thông điệp lỗi chi tiết cho UI đọc
  private LocalDateTime timestamp; // Thời điểm xảy ra lỗi
}
