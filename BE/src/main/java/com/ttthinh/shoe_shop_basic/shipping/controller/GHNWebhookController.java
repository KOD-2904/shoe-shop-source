package com.ttthinh.shoe_shop_basic.shipping.controller;

import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.shipping.service.ShippingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/ghn")
public class GHNWebhookController {
    private final ShippingOrderService shippingOrderService;

    @Value("${ghn.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Map<String, String>> handle(
            @RequestHeader(value = "X-GHN-Webhook-Secret", required = false) String providedSecret,
            @RequestBody Map<String, Object> payload
    ) {
        verifyWebhookSecret(providedSecret);
        shippingOrderService.handleGHNWebhook(payload);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    private void verifyWebhookSecret(String providedSecret) {
        if (webhookSecret == null || webhookSecret.isBlank() || providedSecret == null || providedSecret.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        boolean matches = MessageDigest.isEqual(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
    }
}
