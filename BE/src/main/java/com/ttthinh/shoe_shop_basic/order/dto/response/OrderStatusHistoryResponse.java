package com.ttthinh.shoe_shop_basic.order.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusHistoryResponse {
    String id;
    String orderId;
    String actorId;
    String actorEmail;
    String oldOrderStatus;
    String newOrderStatus;
    String oldPaymentStatus;
    String newPaymentStatus;
    String oldShippingStatus;
    String newShippingStatus;
    String source;
    String note;
    LocalDateTime createdAt;
}
