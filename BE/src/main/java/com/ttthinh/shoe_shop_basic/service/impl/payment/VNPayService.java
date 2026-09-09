package com.ttthinh.shoe_shop_basic.service.impl.payment;

import com.ttthinh.shoe_shop_basic.config.VNPayConfig;
import com.ttthinh.shoe_shop_basic.dto.request.payment.VNPayPaymentRequest;
import com.ttthinh.shoe_shop_basic.dto.response.payment.VNPayCallbackResponse;
import com.ttthinh.shoe_shop_basic.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {
    private final VNPayConfig vnPayConfig;
    // SỬA method lấy IP trong controller
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Nếu là IPv6 localhost (0:0:0:0:0:0:0:1) -> chuyển thành IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        // Nếu có nhiều IP (X-Forwarded-For có thể trả về "client, proxy")
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }
    /**
     * Tạo URL thanh toán VNPAY
     */
    public String createPaymentUrl(VNPayPaymentRequest request, HttpServletRequest httpServletRequest) {
        long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        String txnRef = request.getOrderId() != null
                ? request.getOrderId()
                : VNPayUtil.generateTransactionCode();

        String createDate = getCurrentDate();
        String expireDate = getExpireDate(); // Hết hạn sau 15 phút

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amountInCents));
        params.put("vnp_CurrCode", vnPayConfig.getCurrency());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", request.getOrderInfo()); // Bỏ dấu tiếng Việt
        //params.put("vnp_OrderInfo", removeAccent(request.getOrderInfo())); // Bỏ dấu tiếng Việt
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", vnPayConfig.getLocale());
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", getClientIp(httpServletRequest));
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);
        log.info("Creating VNPay URL for orderId={}, paymentId={}, amount={}, returnUrl={}, configuredIpnUrl={}",
                request.getOrderId(), request.getPaymentId(), request.getAmount(), vnPayConfig.getReturnUrl(), vnPayConfig.getIpnUrl());

        String[] buildData = VNPayUtil.buildQuery(params).split("\\|\\|");
        String hashData = buildData[0];
        String query = buildData[1];

        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        return vnPayConfig.getPaymentUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }
    // Thêm method tính expire date
    private String getExpireDate() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime expire = now.plusMinutes(15);
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return expire.format(formatter);
    }

    // Helper method bỏ dấu tiếng Việt
    private String removeAccent(String text) {
        String result = text.toLowerCase();
        result = result.replaceAll("à|á|ạ|ả|ã|â|ầ|ấ|ậ|ẩ|ẫ|ă|ằ|ắ|ặ|ẳ|ẵ", "a");
        result = result.replaceAll("è|é|ẹ|ẻ|ẽ|ê|ề|ế|ệ|ể|ễ", "e");
        result = result.replaceAll("ì|í|ị|ỉ|ĩ", "i");
        result = result.replaceAll("ò|ó|ọ|ỏ|õ|ô|ồ|ố|ộ|ổ|ỗ|ơ|ờ|ớ|ợ|ở|ỡ", "o");
        result = result.replaceAll("ù|ú|ụ|ủ|ũ|ư|ừ|ứ|ự|ử|ữ", "u");
        result = result.replaceAll("ỳ|ý|ỵ|ỷ|ỹ", "y");
        result = result.replaceAll("đ", "d");
        return result;
    }

    /**
     * Xác thực callback từ VNPAY
     */
    public boolean validateCallback(VNPayCallbackResponse callback) {
        Map<String, String> fields = new HashMap<>();
        fields.put("vnp_TmnCode", callback.getVnp_TmnCode());
        fields.put("vnp_Amount", callback.getVnp_Amount());
        fields.put("vnp_BankCode", callback.getVnp_BankCode());
        fields.put("vnp_BankTranNo", callback.getVnp_BankTranNo());
        fields.put("vnp_CardType", callback.getVnp_CardType());
        fields.put("vnp_OrderInfo", callback.getVnp_OrderInfo());
        fields.put("vnp_PayDate", callback.getVnp_PayDate());
        fields.put("vnp_ResponseCode", callback.getVnp_ResponseCode());
        fields.put("vnp_TxnRef", callback.getVnp_TxnRef());
        fields.put("vnp_TransactionNo", callback.getVnp_TransactionNo());
        fields.put("vnp_TransactionStatus", callback.getVnp_TransactionStatus());

        String[] queryString = VNPayUtil.buildQuery(fields).split("\\|\\|");
        String expectedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString[0]);

        return expectedHash.equals(callback.getVnp_SecureHash());
    }

    public boolean validateParams(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isBlank()) {
            return false;
        }

        Map<String, String> signedFields = new HashMap<>();
        params.forEach((key, value) -> {
            if (key != null
                    && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)) {
                signedFields.put(key, value);
            }
        });

        String[] queryString = VNPayUtil.buildQuery(signedFields).split("\\|\\|");
        String expectedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), queryString[0]);
        return expectedHash.equalsIgnoreCase(secureHash);
    }

    /**
     * Lấy thời gian hiện tại theo format VNPAY (yyyyMMddHHmmss)
     */
    private String getCurrentDate() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter =
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return now.format(formatter);
    }

    /**
     * Lấy IP của client (cần truyền từ request)
     */
    private String getIpAddress() {
        // Tạm thời return localhost
        // Trong controller sẽ lấy từ HttpServletRequest
        return "127.0.0.1";
    }
}
