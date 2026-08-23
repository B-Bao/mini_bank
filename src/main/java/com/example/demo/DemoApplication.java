package com.example.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
@EnableWebSecurity
public class DemoApplication {

  public static void main(String[] args) {
    // Tự động đọc file .env bằng Java thuần và nạp vào System Properties
    loadEnv();

    SpringApplication.run(DemoApplication.class, args);
  }

  private static void loadEnv() {
    File envFile = new File(".env");
    if (!envFile.exists()) {
      return;
    }

    try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
          continue;
        }
        int equalIndex = line.indexOf('=');
        String key = line.substring(0, equalIndex).trim();
        String value = line.substring(equalIndex + 1).trim();

        // Bỏ dấu nháy kép nếu có (ví dụ: DB_NAME="minibank")
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
          value = value.substring(1, value.length() - 1);
        }

        if (System.getProperty(key) == null) {
          System.setProperty(key, value);
        }
      }
    } catch (IOException e) {
      System.err.println("Không thể nạp file .env: " + e.getMessage());
    }
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth
                    // 1. Health check công khai
                    .requestMatchers("/actuator/health")
                    .permitAll()

                    // 2. Swagger & OpenAPI docs công khai
                    .requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**")
                    .permitAll()

                    // 3. Các Actuator còn lại yêu cầu authenticated
                    .requestMatchers("/actuator/**")
                    .authenticated()

                    // 4. Mọi request nghiệp vụ khác
                    .anyRequest()
                    .authenticated())
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }
}
