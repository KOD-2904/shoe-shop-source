package com.ttthinh.shoe_shop_basic;

import com.ttthinh.shoe_shop_basic.controller.shipping.GHNWebhookController;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.enums.ShippingStatus;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.PaymentRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest(properties = "ghn.webhook-secret=test-ghn-secret")
class GHNWebhookIntegrationTest {
    @Autowired
    private GHNWebhookController webhookController;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void webhookRejectsMissingSecret() {
        AppException exception = assertThrows(AppException.class,
                () -> webhookController.handle(null, Map.of(
                        "order_code", "GHN-SECRET-TEST",
                        "status", "delivered"
                )));

        assertEquals(ErrorCode.UNAUTHORIZED_ACCESS, exception.getErrorCode());
    }

    @Test
    void deliveredWebhookMarksOrderDeliveredAndCodPaymentPaid() {
        Order order = createShippingCodOrder("GHN-" + System.nanoTime());

        ResponseEntity<Map<String, String>> response = webhookController.handle("test-ghn-secret", Map.of(
                "order_code", order.getShippingOrderCode(),
                "status", "delivered"
        ));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        Payment updatedPayment = paymentRepository.findDisplayPayments(order.getId()).stream()
                .findFirst()
                .orElseThrow();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ok", response.getBody().get("message"));
        assertEquals(OrderStatus.DELIVERED, updatedOrder.getStatus());
        assertEquals(ShippingStatus.DELIVERED, updatedOrder.getShippingStatus());
        assertNotNull(updatedOrder.getDeliveredAt());
        assertEquals(PaymentStatus.PAID, updatedPayment.getStatus());
        assertNotNull(updatedPayment.getPaidAt());
    }

    private Order createShippingCodOrder(String shippingOrderCode) {
        UserAccount user = new UserAccount();
        user.setEmail("ghn-" + System.nanoTime() + "@example.test");
        user.setPassword("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setProviders(Set.of(AuthProvider.LOCAL));
        user = userAccountRepository.save(user);

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .status(OrderStatus.SHIPPING)
                .shippingStatus(ShippingStatus.DELIVERING)
                .subtotal(BigDecimal.valueOf(100000))
                .discountPrice(BigDecimal.ZERO)
                .totalPrice(BigDecimal.valueOf(100000))
                .shippingFee(BigDecimal.valueOf(30000))
                .finalTotal(BigDecimal.valueOf(130000))
                .voucherUsageCounted(false)
                .receiverName("Test User")
                .phoneNumber("0900000000")
                .shippingAddress("Test address")
                .shippingOrderCode(shippingOrderCode)
                .build());

        paymentRepository.save(Payment.builder()
                .order(order)
                .method(PaymentMethod.COD)
                .status(PaymentStatus.UNPAID)
                .amount(BigDecimal.valueOf(130000))
                .build());

        return order;
    }
}
