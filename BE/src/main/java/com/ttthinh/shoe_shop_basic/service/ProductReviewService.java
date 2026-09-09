package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductReviewRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductReviewResponse;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductReviewSummaryResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;

public interface ProductReviewService {
    ProductReviewSummaryResponse getProductReviews(String productId);

    ProductReviewResponse createOrUpdate(UserAccount user, String productId, ProductReviewRequest request);

    void deleteMyReview(UserAccount user, String productId);

    void deleteMyReview(UserAccount user, String productId, String orderItemId);
}
