package com.ttthinh.shoe_shop_basic.promotion.dto.response;

import com.ttthinh.shoe_shop_basic.promotion.enums.PromotionTargetType;
import com.ttthinh.shoe_shop_basic.promotion.enums.VoucherType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDiscountResponse {
    String id;
    String name;
    PromotionTargetType targetType;
    String targetId;
    VoucherType type;
    BigDecimal value;
    BigDecimal maxDiscountAmount;
    Boolean active;
    LocalDateTime startsAt;
    LocalDateTime endsAt;
}
