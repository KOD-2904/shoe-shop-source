package com.ttthinh.shoe_shop_basic.controller.payment;

import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.payment.VNPayProcessResult;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.service.PaymentApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentApplicationService paymentApplicationService;
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @PostMapping("/vnpay/create")
    public ApiResponse<?> createVNPayPayment(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestParam String orderId,
            HttpServletRequest request
    ) {
        return ApiResponse.builder()
                .result(paymentApplicationService.createVNPayPayment(user, orderId, request))
                .message("Payment created successfully")
                .code(200)
                .build();
    }

    @GetMapping("/vnpay-callback")
    public RedirectView paymentCallback(@RequestParam Map<String, String> params) {
        VNPayProcessResult result = paymentApplicationService.inspectVNPayReturn(params);
        log.info("VNPay callback inspected orderId={}, paymentId={}, validSignature={}, responseCode={}, transactionStatus={}",
                result.getOrderId(), result.getPaymentId(), result.isValidSignature(),
                result.getResponseCode(), result.getTransactionStatus());
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/payment-result")
                .queryParam("orderId", result.getOrderId())
                .queryParam("paymentId", result.getPaymentId())
                .queryParam("code", result.getResponseCode())
                .queryParam("transactionStatus", result.getTransactionStatus())
                .queryParam("validSignature", result.isValidSignature())
                .build()
                .toUriString();
        return new RedirectView(redirectUrl);
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> paymentIPN(@RequestParam Map<String, String> params) {
        return handleIpn(params);
    }

    @PostMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> paymentIPNPost(@RequestParam Map<String, String> params) {
        return handleIpn(params);
    }

    private ResponseEntity<Map<String, String>> handleIpn(Map<String, String> params) {
        VNPayProcessResult result = paymentApplicationService.handleVNPayIpn(params);
        log.info("VNPay IPN processed orderId={}, paymentId={}, validSignature={}, processed={}, rspCode={}, message={}",
                result.getOrderId(), result.getPaymentId(), result.isValidSignature(),
                result.isProcessed(), result.getRspCode(), result.getMessage());
        return ResponseEntity.ok(Map.of("RspCode", result.getRspCode(), "Message", result.getMessage()));
    }
}
