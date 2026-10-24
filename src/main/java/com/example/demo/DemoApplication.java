package com.example.demo;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

  @PostConstruct
  public void init() {
    // Ép JVM chạy chuẩn Asia/Ho_Chi_Minh thay vì Asia/Saigon cũ
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
  }

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
}
