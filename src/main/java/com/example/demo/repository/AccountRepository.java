package com.example.demo.repository;

import com.example.demo.entity.Account; // Entity tương ứng với bảng accounts của bạn
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    List<Account> findByUserId(Long userId);
}
