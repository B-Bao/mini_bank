package com.example.demo.exception.custom;
//404 not found khi không tìm thấy dữ liệu trong DB
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}