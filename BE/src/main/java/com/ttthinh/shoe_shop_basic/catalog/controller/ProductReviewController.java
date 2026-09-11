package com.ttthinh.shoe_shop_basic.catalog.controller;

import com.ttthinh.shoe_shop_basic.catalog.dto.request.ProductReviewRequest;
import com.ttthinh.shoe_shop_basic.common.dto.ApiResponse;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.ProductReviewResponse;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.ProductReviewSummaryResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.catalog.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products/{productId}/reviews")
public class ProductReviewController {
    private final ProductReviewService productReviewService;

    @GetMapping
    public ApiResponse<ProductReviewSummaryResponse> getProductReviews(@PathVariable String productId) {
        return ApiResponse.<ProductReviewSummaryResponse>builder()
                .code(200)
                .message("success")
                .result(productReviewService.getProductReviews(productId))
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<ProductReviewResponse> createOrUpdateMyReview(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String productId,
            @RequestBody @Valid ProductReviewRequest request
    ) {
        return ApiResponse.<ProductReviewResponse>builder()
                .code(200)
                .message("Review saved")
                .result(productReviewService.createOrUpdate(user, productId, request))
                .build();
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMyReview(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String productId,
            @RequestParam(required = false) String orderItemId
    ) {
        productReviewService.deleteMyReview(user, productId, orderItemId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Review deleted")
                .build();
    }
}
