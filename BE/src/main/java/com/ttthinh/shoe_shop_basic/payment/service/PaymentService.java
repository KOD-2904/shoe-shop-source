package com.ttthinh.shoe_shop_basic.payment.service;

import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus;

import java.util.Optional;

public interface PaymentService {
    void updatePaymentStatus(String id, PaymentStatus status);
    public Optional<Payment> findSuccessfulPaymentByOrderId(String orderId);
    public boolean hasSuccessfulPayment(String orderId);
    public long countFailedPayments(String orderId);
    public Optional<Payment> findByPaymentId(String id);
    public Payment updatePayment(Payment payment);

}
