package com.ttthinh.shoe_shop_basic.entity.promotion;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.enums.PromotionTargetType;
import com.ttthinh.shoe_shop_basic.enums.VoucherType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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
@Table(
        name = "product_discounts",
        indexes = {
                @Index(name = "idx_product_discounts_target", columnList = "target_type,target_id"),
                @Index(name = "idx_product_discounts_active_window", columnList = "active,starts_at,ends_at")
        }
)
public class ProductDiscount extends BaseEntity {
    @Column(nullable = false, length = 255)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    PromotionTargetType targetType;

    @Column(name = "target_id", nullable = false)
    String targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    VoucherType type;

    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    BigDecimal value;

    @Column(name = "max_discount_amount", precision = 18, scale = 2)
    BigDecimal maxDiscountAmount;

    @Column(nullable = false)
    Boolean active;

    @Column(name = "starts_at")
    LocalDateTime startsAt;

    @Column(name = "ends_at")
    LocalDateTime endsAt;
}
