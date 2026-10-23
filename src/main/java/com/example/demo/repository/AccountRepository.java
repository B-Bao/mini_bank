package com.example.demo.repository;

import com.example.demo.entity.Account;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query; // Import chuẩn Spring JPA Query
import org.springframework.data.repository.query.Param; // Import chuẩn Spring Data Param
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
  boolean existsByAccountNumber(String accountNumber);

  List<Account> findByUserId(Long userId);

  Optional<Account> findByAccountNumber(String accountNumber);

  // Tìm kiếm và khóa dòng (Pessimistic Lock) chống tranh chấp dữ liệu
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
  Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);
}