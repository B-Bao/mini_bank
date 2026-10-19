package com.example.demo.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private long jwtExpiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes());
  }

  // 1. Sinh JWT từ thông tin Authentication khi đăng nhập thành công
  public String generateToken(Authentication authentication) {
    UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpiration);

    return Jwts.builder()
        .subject(userPrincipal.getUsername())
        .claim(
            "roles",
            authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
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

  public Collection<GrantedAuthority> getAuthoritiesFromJWT(String token) {
    Claims claims =
        Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    Object roles = claims.get("roles");
    if (!(roles instanceof List<?> roleList)) {
      return List.of();
    }

    return roleList.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
        .map(GrantedAuthority.class::cast)
        .toList();
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
