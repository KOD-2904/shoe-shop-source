package com.ttthinh.shoe_shop_basic.service.impl.payment;

import com.ttthinh.shoe_shop_basic.dto.request.payment.VNPayPaymentRequest;
import com.ttthinh.shoe_shop_basic.dto.response.payment.VNPayCallbackResponse;
import com.ttthinh.shoe_shop_basic.dto.response.payment.VNPayProcessResult;
import com.ttthinh.shoe_shop_basic.dto.response.payment.VNPayUrlResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.payment.Payment;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.PaymentRepository;
import com.ttthinh.shoe_shop_basic.service.InventoryLockService;
import com.ttthinh.shoe_shop_basic.service.OrderStatusHistoryService;
import com.ttthinh.shoe_shop_basic.service.PaymentApplicationService;
import com.ttthinh.shoe_shop_basic.service.VoucherService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Payment payment = paymentRepository.findByOrder(order);
        if (payment == null) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (payment.getMethod() != PaymentMethod.VNPAY) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if (payment.getStatus() != PaymentStatus.UNPAID) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        BigDecimal orderTotal = orderTotal(order);
        if (payment.getAmount() == null || payment.getAmount().compareTo(orderTotal) != 0) {
            payment.setAmount(orderTotal);
        }

        VNPayPaymentRequest paymentRequest = VNPayPaymentRequest.builder()
                .orderId(order.getId())
                .paymentId(payment.getId())
                .amount(orderTotal)
                .orderInfo(payment.getId())
                .build();

        String paymentUrl = vnPayService.createPaymentUrl(paymentRequest, request);
        payment.setPaymentUrl(paymentUrl);
        payment.setExpiredAt(LocalDateTime.now().plusMinutes(15));
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
        return VNPayProcessResult.builder()
                .validSignature(validSignature)
                .processed(false)
                .rspCode(validSignature ? "00" : "97")
                .message("Return from VNPAY. Use order detail API for final status.")
                .orderId(callback.getVnp_TxnRef())
                .paymentId(callback.getVnp_OrderInfo())
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
            return result(callback, false, false, "97", "Invalid signature");
        }

        Order order = orderRepository.findByIdForUpdate(callback.getVnp_TxnRef()).orElse(null);
        if (order == null) {
            return result(callback, true, false, "01", "Order not found");
        }

        Payment payment = paymentRepository.findPaymentByIdForUpdate(callback.getVnp_OrderInfo()).orElse(null);
        if (payment == null || !payment.getOrder().getId().equals(order.getId())) {
            return result(callback, true, false, "01", "Payment not found");
        }

        long expectedAmount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact();
        long actualAmount = Long.parseLong(callback.getVnp_Amount());
        if (expectedAmount != actualAmount) {
            return result(callback, true, false, "04", "Invalid amount");
        }

        if (payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.FAILED
                || payment.getTransactionId() != null) {
            return result(callback, true, false, "02", "Order already processed");
        }

        StatusSnapshot before = snapshot(order, payment);
        if ("00".equals(callback.getVnp_ResponseCode()) && "00".equals(callback.getVnp_TransactionStatus())) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setTransactionId(callback.getVnp_TransactionNo());
            payment.setPaidAt(parsePayDate(callback.getVnp_PayDate()));
            order.setStatus(OrderStatus.CONFIRMED);
            inventoryLockService.deductLocked(order);
            paymentRepository.save(payment);
            Order savedOrder = orderRepository.save(order);
            orderStatusHistoryService.record(savedOrder, payment, null, before.orderStatus(), before.paymentStatus(), before.shippingStatus(), "VNPAY_IPN", "VNPay payment success");
            log.info("VNPay payment success orderId={}, paymentId={}", order.getId(), payment.getId());
            return result(callback, true, true, "00", "Success");
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
        return result(callback, true, true, "00", "Payment failed, order updated");
    }

    private VNPayProcessResult result(
            VNPayCallbackResponse callback,
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
                .orderId(callback.getVnp_TxnRef())
                .paymentId(callback.getVnp_OrderInfo())
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
