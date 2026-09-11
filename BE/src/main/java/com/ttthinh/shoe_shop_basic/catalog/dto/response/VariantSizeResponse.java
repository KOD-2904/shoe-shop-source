package com.ttthinh.shoe_shop_basic.catalog.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VariantSizeResponse {
    String id;
    String variantId;
    String variantColor;
    String productId;
    String productName;
    String size;
    String sku;
    BigDecimal price;
    BigDecimal originalPrice;
    BigDecimal salePrice;
    Integer discountPercent;
    Boolean onSale;
    Integer quantity;
}
