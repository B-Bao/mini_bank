package com.example.demo.exception.custom;

// 403 Forbidden đã đăng nhập nhưng không có quyền thực hiện hành động đó
public class ForbiddenException extends RuntimeException {
  public ForbiddenException(String message) {
    super(message);
  }
}
