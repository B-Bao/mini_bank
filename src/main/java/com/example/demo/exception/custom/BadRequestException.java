package com.example.demo.exception.custom;

// 404 bad request lỗi logic nghiệp vụ từ phía người dùng
public class BadRequestException extends RuntimeException {
  public BadRequestException(String message) {
    super(message);
  }
}
