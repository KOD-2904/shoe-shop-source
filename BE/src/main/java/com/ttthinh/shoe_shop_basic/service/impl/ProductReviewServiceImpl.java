package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductReviewRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductReviewResponse;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductReviewSummaryResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductReview;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductReviewImage;
import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderItemRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductReviewRepository;
import com.ttthinh.shoe_shop_basic.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {
    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductReviewSummaryResponse getProductReviews(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        List<ProductReviewResponse> reviews = productReviewRepository.findByProductOrderByCreatedAtDesc(product)
                .stream()
                .map(this::toResponse)
                .toList();
        double averageRating = reviews.stream()
                .mapToInt(ProductReviewResponse::getRating)
                .average()
                .orElse(0);

        return ProductReviewSummaryResponse.builder()
                .productId(product.getId())
                .averageRating(Math.round(averageRating * 10.0) / 10.0)
                .reviewCount(reviews.size())
                .reviews(reviews)
                .build();
    }

    @Override
    @Transactional
    public ProductReviewResponse createOrUpdate(UserAccount user, String productId, ProductReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        OrderItem orderItem = resolveReviewOrderItem(user, productId, request.getOrderItemId());
        ProductReview review = orderItem != null
                ? productReviewRepository.findByOrderItemId(orderItem.getId()).orElseGet(() -> ProductReview.builder()
                    .user(user)
                    .product(product)
                    .orderItem(orderItem)
                    .build())
                : productReviewRepository.findTopByUserAndProductAndOrderItemIsNullOrderByCreatedAtDesc(user, product)
                    .orElseGet(() -> ProductReview.builder()
                            .user(user)
                            .product(product)
                            .build());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.getImages().clear();
        if (request.getImageUrls() != null) {
            request.getImageUrls().stream()
                    .filter(url -> url != null && !url.isBlank())
                    .forEach(url -> review.getImages().add(ProductReviewImage.builder()
                            .review(review)
                            .url(url.trim())
                            .build()));
        }
        return toResponse(productReviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteMyReview(UserAccount user, String productId) {
        deleteMyReview(user, productId, null);
    }

    @Override
    @Transactional
    public void deleteMyReview(UserAccount user, String productId, String orderItemId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        if (orderItemId != null && !orderItemId.isBlank()) {
            productReviewRepository.findByOrderItemId(orderItemId)
                    .filter(review -> review.getUser().getId().equals(user.getId())
                            && review.getProduct().getId().equals(product.getId()))
                    .ifPresent(productReviewRepository::delete);
            return;
        }
        productReviewRepository.findTopByUserAndProductAndOrderItemIsNullOrderByCreatedAtDesc(user, product)
                .ifPresent(productReviewRepository::delete);
    }

    private ProductReviewResponse toResponse(ProductReview review) {
        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .orderItemId(review.getOrderItem() != null ? review.getOrderItem().getId() : null)
                .userId(review.getUser().getId())
                .reviewer(maskEmail(review.getUser().getEmail()))
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrls(review.getImages() == null ? List.of() : review.getImages().stream()
                        .map(ProductReviewImage::getUrl)
                        .toList())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private OrderItem resolveReviewOrderItem(UserAccount user, String productId, String orderItemId) {
        if (orderItemId == null || orderItemId.isBlank()) {
            if (!orderRepository.existsDeliveredOrderForProduct(user, productId)) {
                throw new AppException(ErrorCode.PRODUCT_REVIEW_NOT_ALLOWED);
            }
            return null;
        }
        OrderItem orderItem = orderItemRepository.findDetailById(orderItemId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_REVIEW_NOT_ALLOWED));
        if (!orderItem.getOrder().getUser().getId().equals(user.getId())
                || orderItem.getOrder().getStatus() != OrderStatus.DELIVERED
                || !orderItem.getVariantSize().getVariant().getProduct().getId().equals(productId)) {
            throw new AppException(ErrorCode.PRODUCT_REVIEW_NOT_ALLOWED);
        }
        return orderItem;
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "Customer";
        int atIndex = email.indexOf("@");
        String name = atIndex > 0 ? email.substring(0, atIndex) : email;
        if (name.length() <= 2) return name.charAt(0) + "***";
        return name.substring(0, 2) + "***";
    }
}
