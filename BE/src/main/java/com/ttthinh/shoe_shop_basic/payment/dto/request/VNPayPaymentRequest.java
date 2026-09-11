package com.ttthinh.shoe_shop_basic.payment.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayPaymentRequest {
    String orderId;
    String paymentId;
    String txnRef;
    BigDecimal amount;
    String orderInfo;
    String returnUrl;
    String ipnUrl;
}
