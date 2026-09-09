package com.ttthinh.shoe_shop_basic.service.impl.payment;

import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.PaymentRepository;
import com.ttthinh.shoe_shop_basic.service.InventoryLockService;
import com.ttthinh.shoe_shop_basic.service.OrderStatusHistoryService;
import com.ttthinh.shoe_shop_basic.service.PaymentService;
import com.ttthinh.shoe_shop_basic.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryLockService inventoryLockService;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final VoucherService voucherService;

    @Override
    public void updatePaymentStatus(String id, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findPaymentById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        PaymentStatus oldStatus = payment.getStatus();
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition payment from %s to %s", oldStatus, newStatus)
            );
        }

        payment.setStatus(newStatus);
        if (newStatus == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);
        log.info("Payment {} status updated from {} to {}", id, oldStatus, newStatus);
    }

    @Override
    public Optional<Payment> findSuccessfulPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID);
    }

    @Override
    public boolean hasSuccessfulPayment(String orderId) {
        return paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PAID);
    }

    @Override
    public long countFailedPayments(String orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.FAILED)
                .count();
    }

    @Override
    public Optional<Payment> findByPaymentId(String id) {
        return paymentRepository.findPaymentById(id);
    }

    @Override
    public Payment updatePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    private boolean isValidStatusTransition(PaymentStatus oldStatus, PaymentStatus newStatus) {
        return switch (oldStatus) {
            case UNPAID -> newStatus == PaymentStatus.PAID || newStatus == PaymentStatus.FAILED;
            case PAID -> newStatus == PaymentStatus.REFUNDED;
            case FAILED, REFUNDED -> false;
        };
    }

    @Scheduled(fixedDelayString = "${payment.expiry-scan-delay-ms:120000}")
    @Transactional
    public void handleExpiredPayments() {
        log.debug("Checking for expired payments");
        List<Payment> pendingPayments = paymentRepository.findByStatusAndExpiredAtBefore(PaymentStatus.UNPAID, LocalDateTime.now());
        for (Payment payment : pendingPayments) {
            if (payment.isExpired()) {
                handleExpiredPayment(payment);
            }
        }
    }

    private void handleExpiredPayment(Payment payment) {
        log.info("Payment {} expired", payment.getId());
        Order order = payment.getOrder();
        if (order == null || order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        StatusSnapshot before = snapshot(order, payment);

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Payment timeout");
        paymentRepository.save(payment);

        if (order.getStatus() == OrderStatus.PENDING && !hasSuccessfulPayment(order.getId())) {
            boolean hasActivePayment = paymentRepository.existsByOrderIdAndStatusIn(
                    order.getId(),
                    List.of(PaymentStatus.UNPAID)
            );
            if (!hasActivePayment) {
                inventoryLockService.releaseLocked(order);
                order.setStatus(OrderStatus.CANCELLED);
                releaseVoucherUsage(order);
                orderRepository.save(order);
            }
        }

        orderStatusHistoryService.record(order, payment, null, before.orderStatus(), before.paymentStatus(), before.shippingStatus(), "PAYMENT_TIMEOUT", "Payment expired before completion");
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

    private StatusSnapshot snapshot(Order order, Payment payment) {
        return new StatusSnapshot(
                order != null && order.getStatus() != null ? order.getStatus().name() : null,
                payment != null && payment.getStatus() != null ? payment.getStatus().name() : null,
                order != null && order.getShippingStatus() != null ? order.getShippingStatus().name() : null
        );
    }

    private record StatusSnapshot(String orderStatus, String paymentStatus, String shippingStatus) {
    }
}
