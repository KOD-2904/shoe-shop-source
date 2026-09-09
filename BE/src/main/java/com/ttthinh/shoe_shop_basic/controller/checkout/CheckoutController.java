package com.ttthinh.shoe_shop_basic.controller.checkout;

import com.ttthinh.shoe_shop_basic.dto.request.checkout.CheckoutRequest;
import com.ttthinh.shoe_shop_basic.dto.request.order.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    @PostMapping("/calculate-shipping")
    public ApiResponse<?> calculateShipping(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody @Valid CheckoutRequest checkoutRequest) {
        return ApiResponse.builder()
                .result(checkoutService.preview(user, checkoutRequest))
                .message("Checkout preview created")
                .code(200)
                .build();
    }

    @PostMapping("/preview")
    public ApiResponse<?> preview(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody @Valid CheckoutRequest checkoutRequest) {
        return ApiResponse.builder()
                .result(checkoutService.preview(user, checkoutRequest))
                .message("Checkout preview created")
                .code(200)
                .build();
    }

    @PostMapping("/buy-now-preview")
    public ApiResponse<?> buyNowPreview(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody @Valid BuyNowRequest request) {
        return ApiResponse.builder()
                .result(checkoutService.previewBuyNow(user, request))
                .message("Buy now checkout preview created")
                .code(200)
                .build();
    }
}
