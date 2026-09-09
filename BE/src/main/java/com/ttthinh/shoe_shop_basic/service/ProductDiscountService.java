package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.promotion.ProductDiscountRequest;
import com.ttthinh.shoe_shop_basic.dto.response.promotion.ProductDiscountResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;

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
