package com.ttthinh.shoe_shop_basic.dto.request.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayPaymentRequest {
    private String orderId;           // Mã đơn hàng của merchant
    private String paymentId;
    private BigDecimal amount;        // Số tiền (VNĐ)
    private String orderInfo;         // Thông tin đơn hàng
    private String returnUrl;         // URL trả về sau thanh toán
    private String ipnUrl;            // URL IPN
}
