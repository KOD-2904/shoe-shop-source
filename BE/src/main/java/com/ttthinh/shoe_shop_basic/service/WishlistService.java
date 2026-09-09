package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.response.customer.WishlistItemResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;

import java.util.List;

public interface WishlistService {
    List<WishlistItemResponse> getMyWishlist(UserAccount user);

    WishlistItemResponse addItem(UserAccount user, String productId);

    void removeItem(UserAccount user, String productId);

    boolean contains(UserAccount user, String productId);
}
