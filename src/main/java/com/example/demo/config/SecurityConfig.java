package com.example.demo.config;

import com.example.demo.security.jwt.JwtAuthenticationFilter;
import com.example.demo.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtTokenProvider tokenProvider;
  private final UserDetailsService userDetailsService;

  // Constructor Injection cho các dependency cần thiết
  public SecurityConfig(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
    this.tokenProvider = tokenProvider;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    return new JwtAuthenticationFilter(tokenProvider);
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider =
        new DaoAuthenticationProvider(userDetailsService); // Thêm userDetailsService vào đây!
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  // ĐÃ GỘP THÀNH MỘT BEAN DUY NHẤT: Xử lý sạch lỗi compile và tích hợp bắt lỗi Token tập trung
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 1. Tắt CSRF vì dùng REST API Stateless với JWT
        .csrf(csrf -> csrf.disable())

        // 2. Không lưu Session, hoàn toàn cấu hình Stateless
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 3. Cấu hình phân quyền chi tiết cho các đường dẫn API
        .authorizeHttpRequests(
            auth ->
                auth
                    // Cho phép truy cập công khai vào phân hệ Auth công cộng và phân hệ Error hệ
                    // thống
                    .requestMatchers("/api/v1/auth/**", "/error")
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    // Cho phép truy cập công khai vào Swagger UI để phục vụ test trực quan
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    // Mọi request nghiệp vụ khác (bao gồm /api/v1/accounts) bắt buộc phải kèm Token
                    // hợp lệ
                    .anyRequest()
                    .authenticated())

        // 4. SỬA LỖI HỆ THỐNG: Ngăn chặn cơ chế tự động redirect sang trang GET gây lỗi 500 bậy
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"status\": 401, \"message\": \"Token không hợp lệ, bị thiếu hoặc đã hết hạn sử dụng!\"}");
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"status\": 403, \"message\": \"Bạn không có quyền truy cập tài nguyên này!\"}");
                        }))

        // 5. Nạp cấu hình Authentication Provider hệ thống vào chuỗi lọc
        .authenticationProvider(authenticationProvider())

        // 6. Chèn bộ lọc xử lý chuỗi JWT đứng trước bộ lọc xác thực mặc định mặc định của Spring
        .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
