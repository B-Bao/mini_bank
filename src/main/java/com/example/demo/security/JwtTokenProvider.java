package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  // Đoạn mã bí mật dùng để ký token. Ít nhất 256 bits (32 ký tự).
  private final String JWT_SECRET =
      "minibanksecretkeyminibanksecretkeyminibanksecretkeyminibanksecretkey";

  // Thời gian sống của token: 24 tiếng = 86400000 milliseconds
  private final long JWT_EXPIRATION = 86400000L;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
  }

  // 1. Sinh JWT từ thông tin Authentication khi đăng nhập thành công
  public String generateToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);

    return Jwts.builder()
        .subject(userPrincipal.getUsername())
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey())
        .compact();
  }

  // 2. Lấy username từ chuỗi JWT đã được xác thực
  public String getUsernameFromJWT(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();

    return claims.getSubject();
  }

  // 3. Kiểm tra tính hợp lệ của JWT token
  public boolean validateToken(String authToken) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(authToken);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      // Log lỗi chi tiết khi token không hợp lệ hoặc hết hạn
      System.err.println("Invalid JWT token: " + e.getMessage());
    }
    return false;
  }
}
