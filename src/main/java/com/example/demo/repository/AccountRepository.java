package com.example.demo.repository;

import com.example.demo.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
  boolean existsByAccountNumber(String accountNumber);

  List<Account> findByUserId(Long userId);

  Optional<Account> findByAccountNumber(String accountNumber);
}
