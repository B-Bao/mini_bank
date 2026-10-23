package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "transactions",
    indexes = {
      @Index(name = "idx_bank_tx_ref_number", columnList = "reference_number", unique = true),
      @Index(name = "idx_bank_tx_sender_acc", columnList = "sender_account_number"),
      @Index(name = "idx_bank_tx_receiver_acc", columnList = "receiver_account_number"),
      @Index(name = "idx_bank_tx_status", columnList = "status")
    })
@Getter
@Setter
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "transaction_id", updatable = false, nullable = false)
  private UUID transactionId;

  @Column(name = "reference_number", nullable = false, unique = true, length = 50)
  private String referenceNumber;

  @Column(name = "trace_number", length = 50)
  private String traceNumber;

  // Thông tin người gửi / người nhận
  @Column(name = "sender_account_number", nullable = false, length = 20)
  private String senderAccountNumber;

  @Column(name = "sender_name", nullable = false, length = 100)
  private String senderName;

  @Column(name = "receiver_account_number", nullable = false, length = 20)
  private String receiverAccountNumber;

  @Column(name = "receiver_name", nullable = false, length = 100)
  private String receiverName;

  @Column(name = "beneficiary_bank_code", length = 20)
  private String beneficiaryBankCode = "INTERNAL";

  // Tài chính
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;

  @Column(name = "fee_amount", precision = 18, scale = 2)
  private BigDecimal feeAmount = BigDecimal.ZERO;

  @Column(name = "vat_amount", precision = 18, scale = 2)
  private BigDecimal vatAmount = BigDecimal.ZERO;

  @Column(name = "currency_code", length = 3)
  private String currencyCode = "VND";

  @Column(name = "fee_payer", length = 10)
  private String feePayer; // SENDER hoặc RECEIVER

  // Metadata giao dịch
  @Column(name = "transaction_type", nullable = false, length = 20)
  private String transactionType; // DEPOSIT, WITHDRAW, TRANSFER

  @Column(nullable = false, length = 20)
  private String channel; // MOBILE_APP, INTERNET_BANKING, API

  @Column(nullable = false, length = 20)
  private String status = "PENDING"; // PENDING, SUCCESS, FAILED

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "device_id", length = 100)
  private String deviceId;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "value_date", nullable = false)
  private LocalDate valueDate;

  @Column(name = "completed_at")
  private OffsetDateTime completedAt;
}
