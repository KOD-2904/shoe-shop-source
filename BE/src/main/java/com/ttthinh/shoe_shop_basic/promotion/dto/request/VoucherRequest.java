package com.ttthinh.shoe_shop_basic.promotion.dto.request;

import com.ttthinh.shoe_shop_basic.promotion.enums.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class VoucherRequest {
    @NotBlank
    @Size(max = 64)
    String code;

    @NotBlank
    @Size(max = 255)
    String name;

    @NotNull
    VoucherType type;

    @NotNull
    @DecimalMin(value = "0.01")
    BigDecimal value;

    @Builder.Default
    @DecimalMin(value = "0")
    BigDecimal minOrderAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0")
    BigDecimal maxDiscountAmount;

    @NotNull
    @Min(1)
    Integer usageLimit;

    @Builder.Default
    Boolean active = true;

    LocalDateTime startsAt;
    LocalDateTime endsAt;
}
