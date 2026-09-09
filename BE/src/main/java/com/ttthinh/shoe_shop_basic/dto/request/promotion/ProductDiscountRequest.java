package com.ttthinh.shoe_shop_basic.dto.request.promotion;

import com.ttthinh.shoe_shop_basic.enums.PromotionTargetType;
import com.ttthinh.shoe_shop_basic.enums.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ProductDiscountRequest {
    @NotBlank
    @Size(max = 255)
    String name;

    @NotNull
    PromotionTargetType targetType;

    @NotBlank
    String targetId;

    @NotNull
    VoucherType type;

    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal value;

    @DecimalMin(value = "0")
    BigDecimal maxDiscountAmount;

    @Builder.Default
    Boolean active = true;

    LocalDateTime startsAt;
    LocalDateTime endsAt;
}
