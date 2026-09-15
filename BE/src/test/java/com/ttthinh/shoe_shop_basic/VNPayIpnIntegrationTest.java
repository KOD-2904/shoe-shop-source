package com.ttthinh.shoe_shop_basic;

import com.ttthinh.shoe_shop_basic.config.VNPayConfig;
import com.ttthinh.shoe_shop_basic.payment.dto.response.VNPayProcessResult;
import com.ttthinh.shoe_shop_basic.payment.dto.response.VNPayUrlResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.catalog.entity.Product;
import com.ttthinh.shoe_shop_basic.catalog.entity.ProductVariant;
import com.ttthinh.shoe_shop_basic.catalog.entity.VariantSize;
import com.ttthinh.shoe_shop_basic.inventory.entity.Inventory;
import com.ttthinh.shoe_shop_basic.order.entity.Order;
import com.ttthinh.shoe_shop_basic.order.entity.OrderItem;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.auth.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.order.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.catalog.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.order.enums.ShippingStatus;
import com.ttthinh.shoe_shop_basic.auth.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.inventory.repository.InventoryRepository;
import com.ttthinh.shoe_shop_basic.order.repository.OrderRepository;
import com.ttthinh.shoe_shop_basic.payment.repository.PaymentRepository;
import com.ttthinh.shoe_shop_basic.catalog.repository.ProductRepository;
import com.ttthinh.shoe_shop_basic.catalog.repository.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.auth.repository.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.catalog.repository.VariantSizeRepository;
import com.ttthinh.shoe_shop_basic.payment.service.PaymentApplicationService;
import com.ttthinh.shoe_shop_basic.payment.util.VNPayUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "vnpay.hash-secret=test-vnpay-secret",
        "vnpay.tmn-code=TEST"
})
class VNPayIpnIntegrationTest {
    @Autowired
    private PaymentApplicationService paymentApplicationService;

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private VariantSizeRepository variantSizeRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void createPaymentUrlUsesPaymentTxnRefAndReusesCurrentUrl() {
        TestOrder testOrder = createPendingVNPayOrder(BigDecimal.valueOf(100000), 1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        VNPayUrlResponse first = paymentApplicationService.createVNPayPayment(
                testOrder.order().getUser(),
                testOrder.order().getId(),
                request
        );
        VNPayUrlResponse second = paymentApplicationService.createVNPayPayment(
                testOrder.order().getUser(),
                testOrder.order().getId(),
                request
        );

        Payment payment = paymentRepository.findById(testOrder.payment().getId()).orElseThrow();

        assertEquals(first.getPaymentUrl(), second.getPaymentUrl());
        assertTrue(first.getPaymentUrl().contains("vnp_TxnRef=" + payment.getVnpTxnRef()));
        assertFalse(first.getPaymentUrl().contains("vnp_TxnRef=" + testOrder.order().getId()));
    }

    @Test
    void expiredUnpaidPaymentRegeneratesVnpayUrl() {
        TestOrder testOrder = createPendingVNPayOrder(BigDecimal.valueOf(100000), 1);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        VNPayUrlResponse first = paymentApplicationService.createVNPayPayment(
                testOrder.order().getUser(),
                testOrder.order().getId(),
                request
        );
        Payment payment = paymentRepository.findById(testOrder.payment().getId()).orElseThrow();
        String firstTxnRef = payment.getVnpTxnRef();
        payment.setExpiredAt(LocalDateTime.now().minusMinutes(1));
        paymentRepository.save(payment);

        VNPayUrlResponse regenerated = paymentApplicationService.createVNPayPayment(
                testOrder.order().getUser(),
                testOrder.order().getId(),
                request
        );

        Payment savedPayment = paymentRepository.findById(testOrder.payment().getId()).orElseThrow();
        assertFalse(regenerated.getPaymentUrl().equals(first.getPaymentUrl()));
        assertFalse(savedPayment.getVnpTxnRef().equals(firstTxnRef));
        assertTrue(savedPayment.getExpiredAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void duplicateSuccessIpnDoesNotDeductLockedInventoryTwice() {
        TestOrder testOrder = createPendingVNPayOrder(BigDecimal.valueOf(100000), 2);
        Map<String, String> params = signedSuccessIpn(testOrder.payment().getVnpTxnRef(), "vnp-txn-1", 200000);

        VNPayProcessResult first = paymentApplicationService.handleVNPayIpn(params);
        VNPayProcessResult duplicate = paymentApplicationService.handleVNPayIpn(params);

        assertTrue(first.isProcessed());
        assertEquals("00", first.getRspCode());
        assertFalse(duplicate.isProcessed());
        assertEquals("02", duplicate.getRspCode());

        Payment payment = paymentRepository.findById(testOrder.payment().getId()).orElseThrow();
        Order order = orderRepository.findById(testOrder.order().getId()).orElseThrow();
        Inventory inventory = inventoryRepository.findByVariantSizeId(testOrder.variantSize().getId()).orElseThrow();

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(8, inventory.getQuantity());
        assertEquals(0, inventory.getQuantityLocked());
    }

    @Test
    void successfulIpnAfterLocalTimeoutRequestsRetryInsteadOfMarkingProcessed() {
        TestOrder testOrder = createPendingVNPayOrder(BigDecimal.valueOf(100000), 1);
        Payment payment = testOrder.payment();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Payment timeout");
        paymentRepository.save(payment);
        Map<String, String> params = signedSuccessIpn(payment.getVnpTxnRef(), "vnp-txn-timeout", 100000);

        VNPayProcessResult result = paymentApplicationService.handleVNPayIpn(params);

        assertFalse(result.isProcessed());
        assertEquals("99", result.getRspCode());

        Payment savedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        Order order = orderRepository.findById(testOrder.order().getId()).orElseThrow();

        assertEquals(PaymentStatus.FAILED, savedPayment.getStatus());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    private TestOrder createPendingVNPayOrder(BigDecimal amount, int quantity) {
        UserAccount user = new UserAccount();
        user.setEmail("vnpay-" + System.nanoTime() + "@example.test");
        user.setPassword("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setProviders(Set.of(AuthProvider.LOCAL));
        user = userAccountRepository.save(user);

        Product product = productRepository.save(Product.builder()
                .name("VNPay Test Shoe " + System.nanoTime())
                .slug("vnpay-test-shoe-" + System.nanoTime())
                .basePrice(amount)
                .status(ProductStatus.ACTIVE)
                .build());
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .color("Black")
                .active(true)
                .build());
        VariantSize variantSize = variantSizeRepository.save(VariantSize.builder()
                .variant(variant)
                .size("42")
                .sku("VNPAY-" + System.nanoTime())
                .price(amount)
                .build());
        inventoryRepository.save(Inventory.builder()
                .variantSize(variantSize)
                .quantity(10)
                .quantityLocked(quantity)
                .build());

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingStatus(ShippingStatus.NOT_CREATED)
                .subtotal(amount.multiply(BigDecimal.valueOf(quantity)))
                .discountPrice(BigDecimal.ZERO)
                .totalPrice(amount.multiply(BigDecimal.valueOf(quantity)))
                .shippingFee(BigDecimal.ZERO)
                .finalTotal(amount.multiply(BigDecimal.valueOf(quantity)))
                .voucherUsageCounted(false)
                .receiverName("Test User")
                .phoneNumber("0900000000")
                .shippingAddress("Test address")
                .items(new HashSet<>())
                .build();
        OrderItem item = OrderItem.builder()
                .order(order)
                .variantSize(variantSize)
                .unitPrice(amount)
                .quantity(quantity)
                .lineTotal(amount.multiply(BigDecimal.valueOf(quantity)))
                .build();
        order.getItems().add(item);
        order = orderRepository.save(order);

        Payment payment = paymentRepository.save(Payment.builder()
                .order(order)
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.UNPAID)
                .amount(amount.multiply(BigDecimal.valueOf(quantity)))
                .vnpTxnRef("PAY" + System.nanoTime())
                .build());

        return new TestOrder(order, payment, variantSize);
    }

    private Map<String, String> signedSuccessIpn(String txnRef, String transactionNo, long amount) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TmnCode", "TEST");
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_BankTranNo", "bank-" + transactionNo);
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan don hang");
        params.put("vnp_PayDate", "20260908120000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_TransactionNo", transactionNo);
        params.put("vnp_TransactionStatus", "00");

        String hashData = VNPayUtil.buildQuery(params).split("\\|\\|")[0];
        params.put("vnp_SecureHash", VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData));
        return params;
    }

    private record TestOrder(Order order, Payment payment, VariantSize variantSize) {
    }
}
