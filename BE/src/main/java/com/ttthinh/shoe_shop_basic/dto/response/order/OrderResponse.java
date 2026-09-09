package com.ttthinh.shoe_shop_basic.dto.response.order;

import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {

    private String id;

    private String userId;

    private String status;

    private String shippingStatus;

    private String shippingProvider;

    private String shippingOrderCode;

    private String trackingUrl;

    private BigDecimal totalPrice;

    private BigDecimal shippingPrice;

    private BigDecimal discountPrice;

    private BigDecimal finalTotalPrice;

    private String voucherCode;

    private List<OrderItemResponse> items;

    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String paymentId;
    private String paymentUrl;

    private LocalDateTime createdAt;
    private LocalDateTime shippingCreatedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;

    public BigDecimal getFinalTotalPrice() {
        if (finalTotalPrice != null) {
            return finalTotalPrice.max(BigDecimal.ZERO);
        }
        BigDecimal productTotal = totalPrice != null ? totalPrice : BigDecimal.ZERO;
        BigDecimal shipping = shippingPrice != null ? shippingPrice : BigDecimal.ZERO;
        BigDecimal discount = discountPrice != null ? discountPrice : BigDecimal.ZERO;
        return productTotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
    }
}
