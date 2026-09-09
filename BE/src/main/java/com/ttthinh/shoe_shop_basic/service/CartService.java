package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.cart.AddCartItemRequest;
import com.ttthinh.shoe_shop_basic.dto.response.cart.CartItemResponse;
import com.ttthinh.shoe_shop_basic.dto.response.cart.CartResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;

public interface CartService {
    CartResponse getMyCart(UserAccount user);

    CartItemResponse addItem(UserAccount user, AddCartItemRequest request);

    CartItemResponse updateItem(UserAccount user, String cartItemId, int quantity);

    CartItemResponse removeItem(UserAccount userId, String cartItemId);

    void clearCart(UserAccount user);

}
