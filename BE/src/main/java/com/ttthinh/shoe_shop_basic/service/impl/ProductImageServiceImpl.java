package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductImage;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductImageRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.service.CloudinaryService;
import com.ttthinh.shoe_shop_basic.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    @Override
    public ProductImage addImage(String productId, MultipartFile file, boolean primary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        String imageUrl = cloudinaryService.upload(file, "products/" + productId);
        if(primary) {
            productImageRepository.clearPrimaryByProductId(productId);
        }

        int sortOrder = productImageRepository.findMaxSortOrder(productId);

        ProductImage productImage = ProductImage.builder()
                .product(product)
                .primaryImage(primary)
                .sortOrder(sortOrder)
                .url(imageUrl)
                .build();

        return productImageRepository.save(productImage);
    }
}
