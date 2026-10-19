package com.example.demo.exception.custom;
//401 Unauthorized chưa đăng nhập hoặc token jwt hết hạn/không hợp lệ mà cố truy cập API bảo vệ
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}