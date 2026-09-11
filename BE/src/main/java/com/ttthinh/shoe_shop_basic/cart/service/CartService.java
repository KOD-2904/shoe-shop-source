package com.ttthinh.shoe_shop_basic.cart.service;

import com.ttthinh.shoe_shop_basic.cart.dto.request.AddCartItemRequest;
import com.ttthinh.shoe_shop_basic.cart.dto.response.CartItemResponse;
import com.ttthinh.shoe_shop_basic.cart.dto.response.CartResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;

public interface CartService {
    CartResponse getMyCart(UserAccount user);

    CartItemResponse addItem(UserAccount user, AddCartItemRequest request);

    CartItemResponse updateItem(UserAccount user, String cartItemId, int quantity);

    CartItemResponse removeItem(UserAccount userId, String cartItemId);

    void clearCart(UserAccount user);

}
