package com.ttthinh.shoe_shop_basic.promotion.entity;

import com.ttthinh.shoe_shop_basic.common.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.promotion.enums.VoucherType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "vouchers",
        uniqueConstraints = @UniqueConstraint(name = "uk_vouchers_code", columnNames = "code")
)
public class Voucher extends BaseEntity {
    @Column(nullable = false, length = 64)
    String code;

    @Column(nullable = false, length = 255)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    VoucherType type;

    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    BigDecimal value;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal minOrderAmount;

    @Column(precision = 18, scale = 2)
    BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    Integer usageLimit;

    @Column(nullable = false)
    Integer usedCount;

    @Column(nullable = false)
    Boolean active;

    LocalDateTime startsAt;
    LocalDateTime endsAt;
}
