package com.ttthinh.shoe_shop_basic.catalog.service;

import com.ttthinh.shoe_shop_basic.catalog.dto.request.ProductReviewRequest;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.ProductReviewResponse;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.ProductReviewSummaryResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;

public interface ProductReviewService {
    ProductReviewSummaryResponse getProductReviews(String productId);

    ProductReviewResponse createOrUpdate(UserAccount user, String productId, ProductReviewRequest request);

    void deleteMyReview(UserAccount user, String productId);

    void deleteMyReview(UserAccount user, String productId, String orderItemId);
}
