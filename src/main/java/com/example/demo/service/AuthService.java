package com.example.demo.service;

import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  // file này để xử lý business logic, xử lý các kiểm tra trùng lặp, mã hoá mật khẩu bằng
  // CryptPasswordEncoder, lưu vào DB
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
    // Nếu bảng users của bạn có cột role, có thể set mặc định là "ROLE_USER" ở đây

    // 4. Lưu vào cơ sở dữ liệu
    return userRepository.save(newUser);
  }
}
