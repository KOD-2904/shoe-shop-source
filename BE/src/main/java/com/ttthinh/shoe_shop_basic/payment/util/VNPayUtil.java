package com.ttthinh.shoe_shop_basic.payment.util;

import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.config.VNPayConfig;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class VNPayUtil {

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hashBytes = hmac.doFinal(data.getBytes());
            StringBuilder hash = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                hash.append(String.format("%02x", b & 0xff));
            }
            return hash.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error while hashing", ex);
        }
    }

    public static String buildQuery(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();

        for (String fieldName : fieldNames) {
            String value = params.get(fieldName);
            if (value != null && !value.isEmpty()) {
                String encodedName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII);
                String encodedValue = URLEncoder.encode(value, StandardCharsets.US_ASCII);

                query.append(encodedName).append("=").append(encodedValue).append("&");
                hashData.append(fieldName).append("=").append(encodedValue).append("&");
            }
        }

        if (query.isEmpty() || hashData.isEmpty()) {
            return "||";
        }

        query.setLength(query.length() - 1);
        hashData.setLength(hashData.length() - 1);

        return hashData + "||" + query;
    }

    // Thêm method mới để tạo chuỗi hash (không có & ở đầu)
    public static String createHashString(Map<String, String> params) {
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        Collections.sort(sortedKeys);

        StringBuilder hashStr = new StringBuilder();
        for (String key : sortedKeys) {
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                if (!hashStr.isEmpty()) {
                    hashStr.append("&");
                }
                hashStr.append(key).append("=").append(value);
            }
        }
        return hashStr.toString();
    }
    // Tạo mã hóa đơn ngẫu nhiên (duy nhất trong ngày)
    public static String generateTransactionCode() {
        Random rand = new Random();
        String randomNum = String.format("%06d", rand.nextInt(1000000));
        return System.currentTimeMillis() + randomNum;
    }

    // Convert bytes sang hex string
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void refundFull(VNPayConfig config, Payment payment, String reason) {
        if (payment == null || payment.getMethod() != PaymentMethod.VNPAY
                || payment.getStatus() != PaymentStatus.PAID) {
            return;
        }
        if (payment.getTransactionId() == null || payment.getPaidAt() == null) {
            throw new AppException(ErrorCode.VNPAY_REFUND_FAILED);
        }
        if (payment.getVnpTxnRef() == null || payment.getVnpTxnRef().isBlank()) {
            throw new AppException(ErrorCode.VNPAY_REFUND_FAILED);
        }

        String requestId = payment.getRefundRequestId() != null
                ? payment.getRefundRequestId()
                : payment.getId().replace("-", "");
        payment.setRefundRequestId(requestId);
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String transactionDate = payment.getPaidAt().format(formatter);
        String orderInfo = normalizeRefundInfo(reason, payment.getOrder().getId());

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", config.getVersion());
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", config.getTmnCode());
        body.put("vnp_TransactionType", "02");
        body.put("vnp_TxnRef", payment.getVnpTxnRef());
        body.put("vnp_Amount", payment.getAmount().multiply(java.math.BigDecimal.valueOf(100))
                .toBigIntegerExact().toString());
        body.put("vnp_TransactionNo", payment.getTransactionId());
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateBy", config.getRefundCreateBy());
        body.put("vnp_CreateDate", java.time.LocalDateTime.now().format(formatter));
        body.put("vnp_IpAddr", "127.0.0.1");
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_SecureHash", hmacSHA512(config.getHashSecret(), refundHashData(body)));

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = RestClient.create().post().uri(config.getRefundUrl())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
            if (!"00".equals(value(response, "vnp_ResponseCode"))
                    || !"00".equals(value(response, "vnp_TransactionStatus"))) {
                throw new AppException(ErrorCode.VNPAY_REFUND_FAILED);
            }
            payment.setRefundRequestId(requestId);
            payment.setRefundTransactionId(value(response, "vnp_TransactionNo"));
            payment.setRefundedAt(java.time.LocalDateTime.now());
            payment.setStatus(PaymentStatus.REFUNDED);
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.VNPAY_REFUND_FAILED);
        }
    }

    private static String refundHashData(Map<String, String> body) {
        return String.join("|", body.get("vnp_RequestId"), body.get("vnp_Version"),
                body.get("vnp_Command"), body.get("vnp_TmnCode"), body.get("vnp_TransactionType"),
                body.get("vnp_TxnRef"), body.get("vnp_Amount"), body.get("vnp_TransactionNo"),
                body.get("vnp_TransactionDate"), body.get("vnp_CreateBy"), body.get("vnp_CreateDate"),
                body.get("vnp_IpAddr"), body.get("vnp_OrderInfo"));
    }

    private static String normalizeRefundInfo(String reason, String orderId) {
        String text = reason == null || reason.isBlank() ? "Refund order " + orderId : reason;
        return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D');
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : value.toString();
    }

}
