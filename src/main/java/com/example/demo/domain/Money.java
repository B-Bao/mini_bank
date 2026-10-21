package com.example.demo.domain;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import lombok.Getter;

@Embeddable // Nếu sau này bạn muốn nhúng trực tiếp vào JPA Entity @Embedded
@Getter
public class Money {

  private BigDecimal amount;
  private String currency;

  // Hibernate cần constructor mặc định này
  protected Money() {}

  public Money(BigDecimal amount, String currency) {
    if (currency == null || currency.trim().isEmpty()) {
      throw new IllegalArgumentException("Loại tiền tệ không được để trống");
    }
    this.currency = currency.toUpperCase();
    this.amount = normalizeScale(amount, this.currency);
  }

  /**
   * Tự động chuẩn hóa số chữ số thập phân dựa vào loại tiền tệ: - VND: 0 chữ số thập phân (Làm tròn
   * đến số nguyên) - USD: 2 chữ số thập phân
   */
  private BigDecimal normalizeScale(BigDecimal value, String curr) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    int scale = "VND".equals(curr) ? 0 : 2;
    return value.setScale(scale, RoundingMode.HALF_UP);
  }

  // Nghiệp vụ tính toán tiền tệ an toàn có thể viết ngay tại đây (Domain Logic)
  public Money add(Money other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Không thể cộng hai loại tiền tệ khác nhau: " + this.currency + " và " + other.currency);
    }
    return new Money(this.amount.add(other.amount), this.currency);
  }

  public Money subtract(Money other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Không thể trừ hai loại tiền tệ khác nhau: " + this.currency + " và " + other.currency);
    }
    return new Money(this.amount.subtract(other.amount), this.currency);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Money money = (Money) o;
    return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, currency);
  }
}
