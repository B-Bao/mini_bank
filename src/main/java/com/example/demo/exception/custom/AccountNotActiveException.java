package com.example.demo.exception.custom;

// BẮT BUỘC phải extends RuntimeException
public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(String message) {
        super(message);
    }
}