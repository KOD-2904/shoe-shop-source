package com.ttthinh.shoe_shop_basic.entity.checkout;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "shipping_fee_snapshots")
public class ShippingFeeSnapshot extends BaseEntity {
    @Column(nullable = false)
    String userId;

    @Column(nullable = false)
    String addressId;

    @Column(nullable = false, length = 1000)
    String cartSignature;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal productTotal;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal shippingFee;

    @Column(precision = 18, scale = 2)
    BigDecimal discountAmount;

    @Column(length = 64)
    String voucherCode;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal totalAmount;

    Integer weight;
    Integer length;
    Integer width;
    Integer height;

    @Column(nullable = false)
    LocalDateTime expiresAt;

    LocalDateTime usedAt;

    public boolean isExpired() {
        return expiresAt == null || LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
