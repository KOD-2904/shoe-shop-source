package com.ttthinh.shoe_shop_basic.order.service;

import com.ttthinh.shoe_shop_basic.order.dto.response.OrderStatusHistoryResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.order.entity.Order;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;

import java.util.List;

public interface OrderStatusHistoryService {
    void record(
            Order order,
            Payment payment,
            UserAccount actor,
            String oldOrderStatus,
            String oldPaymentStatus,
            String oldShippingStatus,
            String source,
            String note
    );

    List<OrderStatusHistoryResponse> getHistory(Order order);
}
