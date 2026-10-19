package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accountsid")
    private Long accountsId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_number", nullable = false, unique = true, length = 32)
    private String accountNumber;

    @Column(name = "balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}