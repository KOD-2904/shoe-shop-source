package com.ttthinh.shoe_shop_basic.customer.service.impl;

import com.ttthinh.shoe_shop_basic.catalog.dto.response.ProductResponse;
import com.ttthinh.shoe_shop_basic.customer.dto.response.WishlistItemResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.catalog.entity.Product;
import com.ttthinh.shoe_shop_basic.customer.entity.WishlistItem;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.catalog.mapper.ProductMapper;
import com.ttthinh.shoe_shop_basic.catalog.repository.ProductRepository;
import com.ttthinh.shoe_shop_basic.customer.repository.WishlistItemRepository;
import com.ttthinh.shoe_shop_basic.customer.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getMyWishlist(UserAccount user) {
        return wishlistItemRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public WishlistItemResponse addItem(UserAccount user, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return wishlistItemRepository.findByUserAndProduct(user, product)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(wishlistItemRepository.save(WishlistItem.builder()
                        .user(user)
                        .product(product)
                        .build())));
    }

    @Override
    @Transactional
    public void removeItem(UserAccount user, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        wishlistItemRepository.findByUserAndProduct(user, product)
                .ifPresent(wishlistItemRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contains(UserAccount user, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return wishlistItemRepository.existsByUserAndProduct(user, product);
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        ProductResponse productResponse = productMapper.toProductResponse(item.getProduct());
        return WishlistItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .createdAt(item.getCreatedAt())
                .product(productResponse)
                .build();
    }
}
