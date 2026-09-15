package com.ttthinh.shoe_shop_basic;

import com.ttthinh.shoe_shop_basic.config.VNPayConfig;
import com.ttthinh.shoe_shop_basic.payment.dto.request.VNPayPaymentRequest;
import com.ttthinh.shoe_shop_basic.payment.service.impl.VNPayService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VNPayServiceTest {
    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Test
    void paymentUrlUsesConfiguredVnpayTimezoneAndTimeout() {
        VNPayConfig config = new VNPayConfig();
        config.setTmnCode("TEST");
        config.setHashSecret("test-vnpay-secret");
        config.setPaymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        config.setReturnUrl("https://example.test/api/payment/vnpay-callback");
        config.setVersion("2.1.0");
        config.setCommand("pay");
        config.setOrderType("other");
        config.setCurrency("VND");
        config.setLocale("vn");
        config.setTimeZone(VNPAY_ZONE.getId());
        config.setPaymentTimeoutMinutes(15);

        VNPayService service = new VNPayService(config, ZoneId.of("UTC"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        LocalDateTime before = LocalDateTime.now(VNPAY_ZONE).minusSeconds(1);
        String paymentUrl = service.createPaymentUrl(VNPayPaymentRequest.builder()
                .orderId("order-1")
                .paymentId("payment-1")
                .txnRef("PAY123")
                .amount(BigDecimal.valueOf(100000))
                .orderInfo("Thanh toan don hang order-1")
                .build(), request);
        LocalDateTime after = LocalDateTime.now(VNPAY_ZONE).plusSeconds(1);

        Map<String, String> params = queryParams(paymentUrl);
        LocalDateTime createDate = LocalDateTime.parse(params.get("vnp_CreateDate"), VNPAY_DATE);
        LocalDateTime expireDate = LocalDateTime.parse(params.get("vnp_ExpireDate"), VNPAY_DATE);

        assertFalse(createDate.isBefore(before));
        assertFalse(createDate.isAfter(after));
        assertEquals(createDate.plusMinutes(15), expireDate);
    }

    private Map<String, String> queryParams(String url) {
        return Arrays.stream(URI.create(url).getRawQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(
                        parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                ));
    }
}
