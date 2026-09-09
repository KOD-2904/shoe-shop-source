package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.response.order.OrderStatusHistoryResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;

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
