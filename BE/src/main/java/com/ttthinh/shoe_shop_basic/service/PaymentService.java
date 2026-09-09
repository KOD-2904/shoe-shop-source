package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;

import java.util.Optional;

public interface PaymentService {
    void updatePaymentStatus(String id, PaymentStatus status);
    public Optional<Payment> findSuccessfulPaymentByOrderId(String orderId);
    public boolean hasSuccessfulPayment(String orderId);
    public long countFailedPayments(String orderId);
    public Optional<Payment> findByPaymentId(String id);
    public Payment updatePayment(Payment payment);

}
