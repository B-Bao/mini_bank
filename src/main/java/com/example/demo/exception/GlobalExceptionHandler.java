package com.example.demo.exception;

import com.example.demo.exception.custom.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // 1. Bắt các lỗi RuntimeException thông thường (VD: User đã tồn tại, sai mật khẩu,...)
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // 1. Bắt lỗi không tìm thấy tài nguyên (404 NOT FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
    ErrorResponse error =
        new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }

  // 2. Bắt lỗi trùng lặp dữ liệu / vi phạm tính duy nhất (409 CONFLICT)
  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
    ErrorResponse error =
        new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  // 3. Bắt lỗi nghiệp vụ / Bad Request chung (400 BAD REQUEST)
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
    ErrorResponse error =
        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

  // 4. Bắt lỗi chưa đăng nhập / token không hợp lệ (401 UNAUTHORIZED)
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
    ErrorResponse error =
        new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
  }

  // 5. Bắt lỗi không có quyền thực hiện hành động (403 FORBIDDEN)
  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
    ErrorResponse error =
        new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage(), LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Dữ liệu bị trùng lặp (ví dụ: số tài khoản hoặc email đã tồn tại trong hệ thống).",
            LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.CONFLICT);
  }

  // 2. Bắt các lỗi do `@Valid` (VD: để trống trường, email sai định dạng,...)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  // 3. Bắt tất cả các lỗi hệ thống không lường trước còn lại (Exception chung)
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
    ErrorResponse error =
        new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Lỗi hệ thống nội bộ: " + ex.getMessage(),
            LocalDateTime.now());
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // Bắt lỗi tài khoản không active
  @ExceptionHandler(AccountNotActiveException.class)
  public ResponseEntity<Map<String, Object>> handleAccountNotActive(AccountNotActiveException ex) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("timestamp", LocalDateTime.now());
    errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
    errorResponse.put("error", "Account Not Active");
    errorResponse.put("message", ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }
  // hiện tại đang có 2 exeption chưa được gắn vào
  // 1 là lỗi kiểm tra userID không hề tồn tại trong bảng user ( bị lỗi phân quyền IDOR bắt trước
  // nên lỗi này sẽ không hiển thị)
  // 2 là lỗi chưa phân biệt được loại tiền tệ không được hỗ trợ
}
