package com.ttthinh.shoe_shop_basic.dto.response.checkout;

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
public class CheckoutPreviewResponse {
    String shippingFeeSnapshotId;
    String addressId;
    BigDecimal productTotal;
    BigDecimal shippingFee;
    BigDecimal discountAmount;
    String voucherCode;
    BigDecimal totalAmount;
    LocalDateTime expiresAt;
}
