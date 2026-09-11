package com.ttthinh.shoe_shop_basic.promotion.dto.response;

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
public class VoucherResponse {
    String id;
    String code;
    String name;
    VoucherType type;
    BigDecimal value;
    BigDecimal minOrderAmount;
    BigDecimal maxDiscountAmount;
    Integer usageLimit;
    Integer usedCount;
    Boolean active;
    LocalDateTime startsAt;
    LocalDateTime endsAt;
}
