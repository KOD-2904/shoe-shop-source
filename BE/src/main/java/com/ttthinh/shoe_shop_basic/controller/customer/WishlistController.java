package com.ttthinh.shoe_shop_basic.controller.customer;

import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.customer.WishlistItemResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wishlist")
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    public ApiResponse<List<WishlistItemResponse>> getMyWishlist(
            @AuthenticationPrincipal(expression = "user") UserAccount user
    ) {
        return ApiResponse.<List<WishlistItemResponse>>builder()
                .code(200)
                .message("success")
                .result(wishlistService.getMyWishlist(user))
                .build();
    }

    @PostMapping("/{productId}")
    public ApiResponse<WishlistItemResponse> addItem(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String productId
    ) {
        return ApiResponse.<WishlistItemResponse>builder()
                .code(200)
                .message("Added to wishlist")
                .result(wishlistService.addItem(user, productId))
                .build();
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeItem(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String productId
    ) {
        wishlistService.removeItem(user, productId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Removed from wishlist")
                .build();
    }

    @GetMapping("/{productId}/exists")
    public ApiResponse<Map<String, Boolean>> contains(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String productId
    ) {
        return ApiResponse.<Map<String, Boolean>>builder()
                .code(200)
                .message("success")
                .result(Map.of("wishlisted", wishlistService.contains(user, productId)))
                .build();
    }
}
