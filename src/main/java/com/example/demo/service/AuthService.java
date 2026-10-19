package com.example.demo.service;

import com.example.demo.dto.auth.JwtResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;

  // Xử lý logic Đăng ký: kiểm tra trùng lặp, mã hoá mật khẩu, lưu DB
  public User register(RegisterRequest request) {
    // 1. Kiểm tra username đã tồn tại chưa
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new RuntimeException("Error: Username đã tồn tại!");
    }

    // 2. Kiểm tra email đã tồn tại chưa
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Error: Email đã tồn tại!");
    }

    // 3. Tạo entity User mới và mã hóa mật khẩu
    User newUser = new User();
    newUser.setUsername(request.getUsername());
    newUser.setEmail(request.getEmail());
    newUser.setPassword(passwordEncoder.encode(request.getPassword())); // Mã hóa BCrypt
    newUser.setFullName(request.getFullName());

    // 4. Lưu vào cơ sở dữ liệu
    return userRepository.save(newUser);
  }

  // Xử lý logic Đăng nhập: Xác thực thông tin và cấp phát JWT Token
  public JwtResponse login(LoginRequest loginRequest) {
    // 1. Thực hiện xác thực username và password thông qua AuthenticationManager
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(), loginRequest.getPassword()));

    // 2. Nếu xác thực thành công, lưu thông tin vào SecurityContext
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // 3. Sinh JWT token từ thông tin authentication vừa có
    String jwt = tokenProvider.generateToken(authentication);

    // 4. Trả về token cùng username cho client thông qua DTO
    return new JwtResponse(jwt, loginRequest.getUsername());
  }
}
