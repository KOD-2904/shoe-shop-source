package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.entity.catalog.ProductImage;
import org.springframework.web.multipart.MultipartFile;

public interface ProductImageService {
    public ProductImage addImage(String productId, MultipartFile file, boolean primary);
}
