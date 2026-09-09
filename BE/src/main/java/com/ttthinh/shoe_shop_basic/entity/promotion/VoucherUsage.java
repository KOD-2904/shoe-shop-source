package com.ttthinh.shoe_shop_basic.entity.promotion;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "voucher_usages",
        uniqueConstraints = @UniqueConstraint(name = "uk_voucher_usage_order_code", columnNames = {"order_id", "voucher_code"})
)
public class VoucherUsage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    Voucher voucher;

    @Column(name = "voucher_code", nullable = false, length = 64)
    String voucherCode;

    @Column(name = "order_id", nullable = false)
    String orderId;

    @Column(name = "user_id")
    String userId;

    @Column(name = "released_at")
    LocalDateTime releasedAt;
}
