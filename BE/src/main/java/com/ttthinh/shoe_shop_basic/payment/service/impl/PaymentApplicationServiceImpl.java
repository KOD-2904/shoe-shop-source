package com.ttthinh.shoe_shop_basic.payment.service.impl;

import com.ttthinh.shoe_shop_basic.payment.dto.request.VNPayPaymentRequest;
import com.ttthinh.shoe_shop_basic.payment.dto.response.VNPayCallbackResponse;
import com.ttthinh.shoe_shop_basic.payment.dto.response.VNPayProcessResult;
import com.ttthinh.shoe_shop_basic.payment.dto.response.VNPayUrlResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.order.entity.Order;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.order.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.order.repository.OrderRepository;
import com.ttthinh.shoe_shop_basic.payment.repository.PaymentRepository;
import com.ttthinh.shoe_shop_basic.inventory.service.InventoryLockService;
import com.ttthinh.shoe_shop_basic.order.service.OrderStatusHistoryService;
import com.ttthinh.shoe_shop_basic.payment.service.PaymentApplicationService;
import com.ttthinh.shoe_shop_basic.promotion.service.VoucherService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApplicationServiceImpl implements PaymentApplicationService {
    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryLockService inventoryLockService;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final VoucherService voucherService;

    @Override
    @Transactional
    public VNPayUrlResponse createVNPayPayment(UserAccount user, String orderId, HttpServletRequest request) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        Order order = payment.getOrder();
        if (order == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        if (payment.getMethod() != PaymentMethod.VNPAY) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (payment.getStatus() != PaymentStatus.UNPAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        BigDecimal orderTotal = orderTotal(order);
        if (payment.getAmount() == null || payment.getAmount().compareTo(orderTotal) != 0) {
            payment.setAmount(orderTotal);
        }
        if (payment.getVnpTxnRef() == null || payment.getVnpTxnRef().isBlank()) {
            payment.setVnpTxnRef(buildVnpTxnRef(payment));
        }

        LocalDateTime now = LocalDateTime.now();
        if (StringUtils.hasText(payment.getPaymentUrl())
                && StringUtils.hasText(payment.getVnpTxnRef())
                && payment.getPaymentUrl().contains("vnp_TxnRef=" + payment.getVnpTxnRef())
                && payment.getExpiredAt() != null
                && now.isBefore(payment.getExpiredAt())) {
            return VNPayUrlResponse.builder()
                    .orderId(order.getId())
                    .paymentId(payment.getId())
                    .paymentUrl(payment.getPaymentUrl())
                    .build();
        }
        if (payment.getExpiredAt() != null && !now.isBefore(payment.getExpiredAt())) {
            throw new AppException(ErrorCode.VNPAY_PAYMENT_EXPIRED);
        }

        VNPayPaymentRequest paymentRequest = VNPayPaymentRequest.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .txnRef(payment.getVnpTxnRef())
                .amount(orderTotal)
                .orderInfo("Thanh toan don hang " + compactId(order.getId()))
                .build();

        String paymentUrl = vnPayService.createPaymentUrl(paymentRequest, request);
        payment.setPaymentUrl(paymentUrl);
        payment.setExpiredAt(now.plusMinutes(15));
        paymentRepository.save(payment);

        return VNPayUrlResponse.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    public VNPayProcessResult inspectVNPayReturn(Map<String, String> params) {
        VNPayCallbackResponse callback = toCallback(params);
        boolean validSignature = vnPayService.validateParams(params);
        Payment payment = paymentRepository.findByVnpTxnRef(callback.getVnp_TxnRef()).orElse(null);
        Order order = payment != null ? payment.getOrder() : null;
        return VNPayProcessResult.builder()
                .validSignature(validSignature)
                .processed(false)
                .rspCode(validSignature ? "00" : "97")
                .message("Return from VNPAY. Use order detail API for final status.")
                .orderId(order != null ? order.getId() : null)
                .paymentId(payment != null ? payment.getId() : null)
                .responseCode(callback.getVnp_ResponseCode())
                .transactionStatus(callback.getVnp_TransactionStatus())
                .build();
    }

    @Override
    @Transactional
    public VNPayProcessResult handleVNPayIpn(Map<String, String> params) {
        VNPayCallbackResponse callback = toCallback(params);
        return processVNPayResult(callback, params);
    }

    private VNPayProcessResult processVNPayResult(VNPayCallbackResponse callback, Map<String, String> params) {
        if (!vnPayService.validateParams(params)) {
            return result(callback, null, null, false, false, "97", "Invalid signature");
        }

        if (!vnPayService.matchesTmnCode(callback.getVnp_TmnCode())) {
            return result(callback, null, null, true, false, "97", "Invalid TmnCode");
        }
        if (!StringUtils.hasText(callback.getVnp_TxnRef())) {
            return result(callback, null, null, true, false, "01", "Payment not found");
        }

        Payment payment = paymentRepository.findByVnpTxnRefForUpdate(callback.getVnp_TxnRef()).orElse(null);
        if (payment == null || payment.getOrder() == null) {
            return result(callback, null, null, true, false, "01", "Payment not found");
        }
        Order order = orderRepository.findByIdForUpdate(payment.getOrder().getId()).orElse(payment.getOrder());

        BigInteger expectedAmount = expectedVnpAmount(payment);
        BigInteger actualAmount = parseVnpAmount(callback.getVnp_Amount());
        if (expectedAmount == null || actualAmount == null || !expectedAmount.equals(actualAmount)) {
            return result(callback, order, payment, true, false, "04", "Invalid amount");
        }

        boolean successfulCallback = isSuccessfulCallback(callback);
        if (payment.getStatus() == PaymentStatus.PAID || StringUtils.hasText(payment.getTransactionId())) {
            return result(callback, order, payment, true, false, "02", "Order already processed");
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            if (successfulCallback) {
                log.error("Successful VNPay IPN arrived after local payment failure paymentId={}, orderId={}, txnRef={}",
                        payment.getId(), order.getId(), callback.getVnp_TxnRef());
                return result(callback, order, payment, true, false, "99", "Payment already failed locally");
            }
            return result(callback, order, payment, true, false, "02", "Order already processed");
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            return result(callback, order, payment, true, false, "02", "Order already processed");
        }

        StatusSnapshot before = snapshot(order, payment);
        if (successfulCallback) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setTransactionId(callback.getVnp_TransactionNo());
            payment.setFailureReason(null);
            payment.setPaidAt(parsePayDate(callback.getVnp_PayDate()));
            order.setStatus(OrderStatus.CONFIRMED);
            inventoryLockService.deductLocked(order);
            paymentRepository.save(payment);
            Order savedOrder = orderRepository.save(order);
            orderStatusHistoryService.record(savedOrder, payment, null, before.orderStatus(), before.paymentStatus(), before.shippingStatus(), "VNPAY_IPN", "VNPay payment success");
            log.info("VNPay payment success orderId={}, paymentId={}", order.getId(), payment.getId());
            return result(callback, savedOrder, payment, true, true, "00", "Success");
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setTransactionId(callback.getVnp_TransactionNo());
        payment.setFailureReason(getResponseCodeMessage(callback.getVnp_ResponseCode()));
        if (order.getStatus() == OrderStatus.PENDING) {
            inventoryLockService.releaseLocked(order);
        }
        order.setStatus(OrderStatus.CANCELLED);
        releaseVoucherUsage(order);
        paymentRepository.save(payment);
        Order savedOrder = orderRepository.save(order);
        orderStatusHistoryService.record(savedOrder, payment, null, before.orderStatus(), before.paymentStatus(), before.shippingStatus(), "VNPAY_IPN", "VNPay payment failed: " + callback.getVnp_ResponseCode());
        log.info("VNPay payment failed orderId={}, paymentId={}, responseCode={}",
                order.getId(), payment.getId(), callback.getVnp_ResponseCode());
        return result(callback, savedOrder, payment, true, true, "00", "Payment failed, order updated");
    }

    private VNPayProcessResult result(
            VNPayCallbackResponse callback,
            Order order,
            Payment payment,
            boolean validSignature,
            boolean processed,
            String rspCode,
            String message
    ) {
        return VNPayProcessResult.builder()
                .validSignature(validSignature)
                .processed(processed)
                .rspCode(rspCode)
                .message(message)
                .orderId(order != null ? order.getId() : null)
                .paymentId(payment != null ? payment.getId() : null)
                .responseCode(callback.getVnp_ResponseCode())
                .transactionStatus(callback.getVnp_TransactionStatus())
                .build();
    }

    private BigDecimal orderTotal(Order order) {
        BigDecimal productTotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountPrice() != null ? order.getDiscountPrice() : BigDecimal.ZERO;
        return productTotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
    }

    private String buildVnpTxnRef(Payment payment) {
        return "PAY" + compactId(payment.getId());
    }

    private String compactId(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "");
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
                order.getStatus() != null ? order.getStatus().name() : null,
                payment != null && payment.getStatus() != null ? payment.getStatus().name() : null,
                order.getShippingStatus() != null ? order.getShippingStatus().name() : null
        );
    }

    private VNPayCallbackResponse toCallback(Map<String, String> params) {
        return VNPayCallbackResponse.builder()
                .vnp_TmnCode(params.get("vnp_TmnCode"))
                .vnp_Amount(params.get("vnp_Amount"))
                .vnp_BankCode(params.get("vnp_BankCode"))
                .vnp_BankTranNo(params.get("vnp_BankTranNo"))
                .vnp_CardType(params.get("vnp_CardType"))
                .vnp_OrderInfo(params.get("vnp_OrderInfo"))
                .vnp_PayDate(params.get("vnp_PayDate"))
                .vnp_ResponseCode(params.get("vnp_ResponseCode"))
                .vnp_TxnRef(params.get("vnp_TxnRef"))
                .vnp_TransactionNo(params.get("vnp_TransactionNo"))
                .vnp_TransactionStatus(params.get("vnp_TransactionStatus"))
                .vnp_SecureHash(params.get("vnp_SecureHash"))
                .build();
    }

    private LocalDateTime parsePayDate(String payDate) {
        try {
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(payDate, formatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private BigInteger expectedVnpAmount(Payment payment) {
        try {
            return payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact();
        } catch (Exception exception) {
            log.error("Payment amount cannot be represented as VNPay amount paymentId={}, amount={}",
                    payment.getId(), payment.getAmount());
            return null;
        }
    }

    private BigInteger parseVnpAmount(String amount) {
        if (!StringUtils.hasText(amount)) {
            return null;
        }
        try {
            return new BigInteger(amount);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isSuccessfulCallback(VNPayCallbackResponse callback) {
        return "00".equals(callback.getVnp_ResponseCode())
                && "00".equals(callback.getVnp_TransactionStatus());
    }

    private String getResponseCodeMessage(String code) {
        Map<String, String> messages = new HashMap<>();
        messages.put("00", "Transaction successful");
        messages.put("01", "Transaction already exists");
        messages.put("02", "Invalid merchant");
        messages.put("03", "Invalid request data");
        messages.put("04", "Invalid signature");
        messages.put("05", "Invalid request");
        messages.put("06", "Invalid params");
        messages.put("07", "Duplicated transaction");
        messages.put("08", "Transaction failed");
        messages.put("09", "Suspected fraud");
        messages.put("10", "Technical error");
        messages.put("11", "Payment expired");
        messages.put("12", "Transaction cancelled");
        messages.put("24", "Customer cancelled transaction");
        return messages.getOrDefault(code, "Unknown payment error");
    }

    private record StatusSnapshot(String orderStatus, String paymentStatus, String shippingStatus) {
    }
}
