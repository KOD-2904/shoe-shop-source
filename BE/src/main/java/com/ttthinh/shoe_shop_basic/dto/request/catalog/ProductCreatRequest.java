package com.ttthinh.shoe_shop_basic.dto.request.catalog;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductCreatRequest {
    String name;
    String slug;
    String description;
    String brandId;
    String categoryId;
    BigDecimal basePrice;
    //String status; // DRAFT | ACTIVE | INACTIVE
}
