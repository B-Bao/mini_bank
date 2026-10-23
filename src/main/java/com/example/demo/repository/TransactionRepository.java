package com.example.demo.repository;

import com.example.demo.entity.Transaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
  // Kiểm tra idempotency (chống trùng lặp mã giao dịch)
  boolean existsByReferenceNumber(String referenceNumber);
}
