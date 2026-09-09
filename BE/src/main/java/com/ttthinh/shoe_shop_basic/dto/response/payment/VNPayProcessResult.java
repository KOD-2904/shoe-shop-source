package com.ttthinh.shoe_shop_basic.dto.response.payment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayProcessResult {
    boolean validSignature;
    boolean processed;
    String rspCode;
    String message;
    String orderId;
    String paymentId;
    String responseCode;
    String transactionStatus;
}
