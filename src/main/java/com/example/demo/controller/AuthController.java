package com.example.demo.controller;

import com.example.demo.dto.JwtResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;

  public AuthController(
      AuthService authService,
      AuthenticationManager authenticationManager,
      JwtTokenProvider tokenProvider) {
    this.authService = authService;
    this.authenticationManager = authenticationManager;
    this.tokenProvider = tokenProvider;
  }

  // 1. Endpoint Đăng ký
  @PostMapping("/register")
  public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
    authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body("Đăng ký tài khoản thành công!");
  }

  // 2. Endpoint Đăng nhập (Cấp JWT Token)
  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
    // Thực hiện xác thực username và password thông qua AuthenticationManager
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), loginRequest.getPassword()));

    // Nếu xác thực thành công, lưu thông tin vào SecurityContext
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Sinh JWT token từ thông tin authentication vừa có
    String jwt = tokenProvider.generateToken(authentication);

    // Trả về token cho client
    return ResponseEntity.ok(new JwtResponse(jwt, loginRequest.getUsername()));
  }
}
