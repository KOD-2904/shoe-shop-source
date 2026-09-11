package com.ttthinh.shoe_shop_basic.payment.service.impl;

import com.ttthinh.shoe_shop_basic.config.VNPayConfig;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.payment.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayRefundService {
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> ACCEPTED_STATUSES = Set.of("00", "05", "06");

    private final VNPayConfig config;
    private final RestClient restClient;

    public void refundFull(Payment payment, String reason) {
        if (payment == null || payment.getMethod() != PaymentMethod.VNPAY
                || payment.getStatus() != PaymentStatus.PAID) {
            return;
        }
        if (payment.getTransactionId() == null || payment.getPaidAt() == null) {
            throw new AppException(ErrorCode.VNPAY_REFUND_FAILED);
        }
        String originalTxnRef = originalTxnRef(payment);
        if (originalTxnRef == null || originalTxnRef.isBlank()) {
            throw new AppException(ErrorCode.VNPAY_REFUND_FAILED);
        }

        String requestId = payment.getRefundRequestId() != null
                ? payment.getRefundRequestId()
                : payment.getId().replace("-", "");
        String createDate = LocalDateTime.now().format(VNPAY_DATE);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", config.getVersion());
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", config.getTmnCode());
        body.put("vnp_TransactionType", "02");
        body.put("vnp_TxnRef", originalTxnRef);
        body.put("vnp_Amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
        body.put("vnp_TransactionNo", payment.getTransactionId());
        body.put("vnp_TransactionDate", originalTransactionDate(payment));
        body.put("vnp_CreateBy", config.getRefundCreateBy());
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", "127.0.0.1");
        body.put("vnp_OrderInfo", normalize(reason));
        body.put("vnp_SecureHash", VNPayUtil.hmacSHA512(config.getHashSecret(), hashData(body)));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post().uri(config.getRefundUrl())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
            String responseCode = value(response, "vnp_ResponseCode");
            String transactionStatus = value(response, "vnp_TransactionStatus");
            if (!"00".equals(responseCode) || !ACCEPTED_STATUSES.contains(transactionStatus)) {
                log.warn("VNPAY refund rejected paymentId={}, responseCode={}, transactionStatus={}, message={}",
                        payment.getId(), responseCode, transactionStatus, value(response, "vnp_Message"));
                throw new AppException(ErrorCode.VNPAY_REFUND_FAILED);
            }
            log.info("VNPAY refund accepted paymentId={}, transactionStatus={}", payment.getId(), transactionStatus);
            payment.setRefundRequestId(requestId);
            payment.setRefundTransactionId(value(response, "vnp_TransactionNo"));
            payment.setRefundedAt(LocalDateTime.now());
            payment.setStatus(PaymentStatus.REFUNDED);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("VNPAY refund request failed paymentId={}: {}", payment.getId(), exception.getMessage());
            throw new AppException(ErrorCode.VNPAY_REFUND_FAILED);
        }
    }

    private String originalTransactionDate(Payment payment) {
        String createDate = paymentUrlParameter(payment, "vnp_CreateDate");
        if (createDate != null && !createDate.isBlank()) {
            return createDate;
        }
        return payment.getPaidAt().format(VNPAY_DATE);
    }

    private String originalTxnRef(Payment payment) {
        if (payment.getVnpTxnRef() != null && !payment.getVnpTxnRef().isBlank()) {
            return payment.getVnpTxnRef();
        }
        return paymentUrlParameter(payment, "vnp_TxnRef");
    }

    private String paymentUrlParameter(Payment payment, String name) {
        try {
            String rawQuery = URI.create(payment.getPaymentUrl()).getRawQuery();
            if (rawQuery != null) {
                for (String parameter : rawQuery.split("&")) {
                    String[] parts = parameter.split("=", 2);
                    if (parts.length == 2 && name.equals(parts[0])) {
                        return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception exception) {
            log.warn("Cannot read original {} for paymentId={}", name, payment.getId());
        }
        return null;
    }

    private String hashData(Map<String, String> body) {
        return String.join("|", body.get("vnp_RequestId"), body.get("vnp_Version"),
                body.get("vnp_Command"), body.get("vnp_TmnCode"), body.get("vnp_TransactionType"),
                body.get("vnp_TxnRef"), body.get("vnp_Amount"), body.get("vnp_TransactionNo"),
                body.get("vnp_TransactionDate"), body.get("vnp_CreateBy"), body.get("vnp_CreateDate"),
                body.get("vnp_IpAddr"), body.get("vnp_OrderInfo"));
    }

    private String normalize(String reason) {
        return java.text.Normalizer.normalize(reason, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D');
    }

    private String value(Map<String, Object> map, String key) {
        Object result = map == null ? null : map.get(key);
        return result == null ? null : result.toString();
    }
}
