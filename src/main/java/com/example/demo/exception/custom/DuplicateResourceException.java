package com.example.demo.exception.custom;
//409 conflict vi phạm tính duy nhất
public class DuplicateResourceException extends RuntimeException {
  public DuplicateResourceException(String message) {
    super(message);
  }
}