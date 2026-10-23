package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reference_code", unique = true, nullable = false)
  private String referenceCode;

  @Column(name = "source_account_id", nullable = false)
  private Long sourceAccountId;

  @Column(name = "destination_account_id", nullable = false)
  private Long destinationAccountId;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal amount;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal fee;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 20)
  private String status; // SUCCESS, PENDING, FAILED

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onCreateUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
