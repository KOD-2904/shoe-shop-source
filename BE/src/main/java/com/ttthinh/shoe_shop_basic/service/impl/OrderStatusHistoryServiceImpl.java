package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.response.order.OrderStatusHistoryResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.order.OrderStatusHistory;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderStatusHistoryRepository;
import com.ttthinh.shoe_shop_basic.service.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderStatusHistoryServiceImpl implements OrderStatusHistoryService {
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Override
    @Transactional
    public void record(
            Order order,
            Payment payment,
            UserAccount actor,
            String oldOrderStatus,
            String oldPaymentStatus,
            String oldShippingStatus,
            String source,
            String note
    ) {
        String newOrderStatus = order.getStatus() != null ? order.getStatus().name() : null;
        String newPaymentStatus = payment != null && payment.getStatus() != null ? payment.getStatus().name() : null;
        String newShippingStatus = order.getShippingStatus() != null ? order.getShippingStatus().name() : null;
        boolean changed = !Objects.equals(oldOrderStatus, newOrderStatus)
                || !Objects.equals(oldPaymentStatus, newPaymentStatus)
                || !Objects.equals(oldShippingStatus, newShippingStatus);
        if (!changed) {
            return;
        }

        orderStatusHistoryRepository.save(OrderStatusHistory.builder()
                .order(order)
                .actor(actor)
                .oldOrderStatus(oldOrderStatus)
                .newOrderStatus(newOrderStatus)
                .oldPaymentStatus(oldPaymentStatus)
                .newPaymentStatus(newPaymentStatus)
                .oldShippingStatus(oldShippingStatus)
                .newShippingStatus(newShippingStatus)
                .source(source)
                .note(note)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getHistory(Order order) {
        return orderStatusHistoryRepository.findByOrderOrderByCreatedAtAsc(order)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private OrderStatusHistoryResponse toResponse(OrderStatusHistory history) {
        UserAccount actor = history.getActor();
        return OrderStatusHistoryResponse.builder()
                .id(history.getId())
                .orderId(history.getOrder().getId())
                .actorId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .oldOrderStatus(history.getOldOrderStatus())
                .newOrderStatus(history.getNewOrderStatus())
                .oldPaymentStatus(history.getOldPaymentStatus())
                .newPaymentStatus(history.getNewPaymentStatus())
                .oldShippingStatus(history.getOldShippingStatus())
                .newShippingStatus(history.getNewShippingStatus())
                .source(history.getSource())
                .note(history.getNote())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
