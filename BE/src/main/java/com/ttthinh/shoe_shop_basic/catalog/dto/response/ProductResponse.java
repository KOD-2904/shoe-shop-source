package com.ttthinh.shoe_shop_basic.catalog.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {
    String id;
    String name;
    String slug;
    String description;
    String brandId;
    String brandName;
    String categoryId;
    String categoryName;
    BigDecimal basePrice;
    BigDecimal originalPrice;
    BigDecimal salePrice;
    Integer discountPercent;
    Boolean onSale;
    String status; // DRAFT | ACTIVE | INACTIVE
    String primaryImageUrl;
    List<String> imageUrls;
}
