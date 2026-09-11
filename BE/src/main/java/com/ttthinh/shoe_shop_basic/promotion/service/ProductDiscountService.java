package com.ttthinh.shoe_shop_basic.promotion.service;

import com.ttthinh.shoe_shop_basic.promotion.dto.request.ProductDiscountRequest;
import com.ttthinh.shoe_shop_basic.promotion.dto.response.ProductDiscountResponse;
import com.ttthinh.shoe_shop_basic.catalog.entity.Product;
import com.ttthinh.shoe_shop_basic.catalog.entity.VariantSize;

import java.math.BigDecimal;
import java.util.List;

public interface ProductDiscountService {
    List<ProductDiscountResponse> getAll();

    ProductDiscountResponse create(ProductDiscountRequest request);

    ProductDiscountResponse setActive(String id, boolean active);

    BigDecimal effectivePrice(VariantSize variantSize);

    BigDecimal effectiveProductPrice(Product product);

    BigDecimal discountAmount(Product product, BigDecimal basePrice);
}
