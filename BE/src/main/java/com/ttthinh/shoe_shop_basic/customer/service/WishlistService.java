package com.ttthinh.shoe_shop_basic.customer.service;

import com.ttthinh.shoe_shop_basic.customer.dto.response.WishlistItemResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;

import java.util.List;

public interface WishlistService {
    List<WishlistItemResponse> getMyWishlist(UserAccount user);

    WishlistItemResponse addItem(UserAccount user, String productId);

    void removeItem(UserAccount user, String productId);

    boolean contains(UserAccount user, String productId);
}
