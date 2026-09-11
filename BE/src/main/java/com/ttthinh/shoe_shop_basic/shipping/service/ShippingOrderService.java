package com.ttthinh.shoe_shop_basic.shipping.service;

import com.ttthinh.shoe_shop_basic.order.dto.response.OrderResponse;
import com.ttthinh.shoe_shop_basic.order.entity.Order;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.order.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.order.enums.ShippingStatus;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.order.mapper.OrderMapper;
import com.ttthinh.shoe_shop_basic.order.repository.OrderRepository;
import com.ttthinh.shoe_shop_basic.payment.repository.PaymentRepository;
import com.ttthinh.shoe_shop_basic.order.service.OrderStatusHistoryService;
import com.ttthinh.shoe_shop_basic.promotion.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingOrderService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final GHNShippingService ghnShippingService;
    private final OrderMapper orderMapper;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final VoucherService voucherService;
    private final GHNShippingModeService shippingModeService;

    @Value("${ghn.polling-enabled:false}")
    private boolean pollingEnabled;

    @Transactional
    public OrderResponse createGHNOrder(String orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.READY_TO_SHIP) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (order.getShippingStatus() != null && order.getShippingStatus() != ShippingStatus.NOT_CREATED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = getDisplayPayment(order);
        if (payment == null) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (payment.getMethod() != PaymentMethod.COD && payment.getStatus() != PaymentStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        StatusSnapshot before = snapshot(order, payment);
        boolean mockTest = shippingModeService.isMockTest();
        String orderCode = ghnShippingService.createOrder(order, payment);
        order.setShippingProvider(mockTest ? "GHN_MOCK_TEST" : "GHN");
        order.setShippingOrderCode(orderCode);
        order.setShippingStatus(ShippingStatus.CREATED);
        order.setShippingCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPING);

        Order savedOrder = orderRepository.save(order);
        orderStatusHistoryService.record(
                savedOrder,
                payment,
                null,
                before.orderStatus(),
                before.paymentStatus(),
                before.shippingStatus(),
                mockTest ? "GHN_MOCK_CREATE" : "GHN_CREATE",
                mockTest ? "Mock GHN handoff created" : "GHN shipping order created"
        );
        OrderResponse response = orderMapper.toOrderResponse(savedOrder);
        if (payment != null) {
            response.setPaymentId(payment.getId());
            response.setPaymentMethod(payment.getMethod());
            response.setPaymentStatus(payment.getStatus());
            response.setPaymentUrl(payment.getPaymentUrl());
        }
        return response;
    }

    @Transactional
    public void handleGHNWebhook(Map<String, Object> payload) {
        String orderCode = getString(payload, "order_code");
        String status = getString(payload, "status");
        if (orderCode == null || status == null) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Order order = orderRepository.findByShippingOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        Payment payment = getDisplayPayment(order);
        StatusSnapshot before = snapshot(order, payment);
        ShippingStatus shippingStatus = mapGHNStatus(status);
        if (order.getStatus().isFinal() && order.getShippingStatus() != shippingStatus) {
            return;
        }
        order.setShippingStatus(shippingStatus);

        if (shippingStatus == ShippingStatus.PICKED || shippingStatus == ShippingStatus.DELIVERING) {
            order.setStatus(OrderStatus.SHIPPING);
            if (order.getShippedAt() == null) {
                order.setShippedAt(LocalDateTime.now());
            }
        } else if (shippingStatus == ShippingStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
            payment = markCodPaid(order);
        } else if (shippingStatus == ShippingStatus.DELIVERY_FAILED) {
            order.setStatus(OrderStatus.FAILED);
            payment = failUnpaidCodPayment(payment, "Delivery failed");
            releaseVoucherUsage(order);
        } else if (shippingStatus == ShippingStatus.RETURNED) {
            order.setStatus(OrderStatus.RETURNED);
            payment = failUnpaidCodPayment(payment, "Order returned");
            releaseVoucherUsage(order);
        } else if (shippingStatus == ShippingStatus.CANCELLED) {
            order.setStatus(OrderStatus.CANCELLED);
            payment = failUnpaidCodPayment(payment, "Shipping order cancelled");
            releaseVoucherUsage(order);
        }

        Order savedOrder = orderRepository.save(order);
        orderStatusHistoryService.record(savedOrder, payment, null, before.orderStatus(), before.paymentStatus(), before.shippingStatus(), "GHN_WEBHOOK", "GHN status: " + status);
    }

    private Payment markCodPaid(Order order) {
        Payment payment = getDisplayPayment(order);
        if (payment != null && payment.getMethod() == PaymentMethod.COD && payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
        }
        return payment;
    }

    private Payment failUnpaidCodPayment(Payment payment, String reason) {
        if (payment != null && payment.getMethod() == PaymentMethod.COD && payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            return paymentRepository.save(payment);
        }
        return payment;
    }

    private void releaseVoucherUsage(Order order) {
        if (order == null
                || !Boolean.TRUE.equals(order.getVoucherUsageCounted())
                || order.getVoucherUsageReleasedAt() != null) {
            return;
        }
        voucherService.releaseUsed(order.getVoucherCode(), order.getId());
        order.setVoucherUsageCounted(false);
        order.setVoucherUsageReleasedAt(LocalDateTime.now());
    }

    private Payment getDisplayPayment(Order order) {
        return paymentRepository.findDisplayPayments(order.getId()).stream()
                .findFirst()
                .orElse(null);
    }

    private ShippingStatus mapGHNStatus(String ghnStatus) {
        return switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick", "created" -> ShippingStatus.CREATED;
            case "picking" -> ShippingStatus.PICKING;
            case "picked" -> ShippingStatus.PICKED;
            case "delivering", "transporting", "sorting" -> ShippingStatus.DELIVERING;
            case "delivered" -> ShippingStatus.DELIVERED;
            case "delivery_fail", "delivery_failed", "waiting_to_return" -> ShippingStatus.DELIVERY_FAILED;
            case "return", "returning" -> ShippingStatus.RETURNING;
            case "returned" -> ShippingStatus.RETURNED;
            case "cancel", "cancelled", "canceled" -> ShippingStatus.CANCELLED;
            default -> throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        };
    }

    private String getString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }

    private StatusSnapshot snapshot(Order order, Payment payment) {
        return new StatusSnapshot(
                order.getStatus() != null ? order.getStatus().name() : null,
                payment != null && payment.getStatus() != null ? payment.getStatus().name() : null,
                order.getShippingStatus() != null ? order.getShippingStatus().name() : null
        );
    }

    private record StatusSnapshot(String orderStatus, String paymentStatus, String shippingStatus) {
    }

    @Scheduled(fixedDelayString = "${ghn.polling-delay-ms:300000}")
    @Transactional
    public void syncActiveGHNOrders() {
        if (!pollingEnabled || shippingModeService.isMockTest()) {
            return;
        }
        orderRepository.findByStatus(OrderStatus.SHIPPING).stream()
                .filter(order -> order.getShippingOrderCode() != null && !order.getShippingOrderCode().isBlank())
                .forEach(order -> {
                    String status = ghnShippingService.getOrderStatus(order.getShippingOrderCode());
                    if (status == null || status.isBlank()) {
                        return;
                    }
                    try {
                        handleGHNWebhook(Map.of("order_code", order.getShippingOrderCode(), "status", status));
                    } catch (AppException exception) {
                        log.warn("GHN polling ignored orderId={}, orderCode={}, status={}",
                                order.getId(), order.getShippingOrderCode(), status);
                    }
                });
    }
}
