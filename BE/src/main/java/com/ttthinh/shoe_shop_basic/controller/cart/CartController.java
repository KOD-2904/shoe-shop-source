package com.ttthinh.shoe_shop_basic.controller.cart;

import com.ttthinh.shoe_shop_basic.dto.request.cart.AddCartItemRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.cart.CartItemResponse;
import com.ttthinh.shoe_shop_basic.dto.response.cart.CartResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;
    @GetMapping
    public CartResponse getMyCart(
            //Hoặc dùng
            //@AuthenticationPrincipal CustomUserDetails userDetails
            @AuthenticationPrincipal(expression = "user") UserAccount user
    ) {
        return cartService.getMyCart(user);
    }

    /* ================= ADD ITEM ================= */

    @PostMapping("/items")
    public void addItem(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody @Valid AddCartItemRequest request
    ) {
        cartService.addItem(user, request);
    }

    /* ================= UPDATE ITEM ================= */

    @PutMapping("/items/{cartItemId}")
    public CartItemResponse updateItem(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String cartItemId,
            @RequestParam int quantity
    ) {
        return cartService.updateItem(user, cartItemId, quantity);
    }

    /* ================= REMOVE ITEM ================= */

    @DeleteMapping("/items/{cartItemId}")
    public CartItemResponse removeItem(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String cartItemId
    ) {
        return cartService.removeItem(user, cartItemId);
    }

    /* ================= CLEAR CART ================= */

    @DeleteMapping
    public ApiResponse clearCart(
            @AuthenticationPrincipal(expression = "user") UserAccount user
    ) {
        cartService.clearCart(user);
        return ApiResponse.builder().code(200).message("success").build();
    }
}
