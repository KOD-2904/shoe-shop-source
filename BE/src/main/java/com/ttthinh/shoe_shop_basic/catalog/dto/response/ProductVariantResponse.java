package com.ttthinh.shoe_shop_basic.catalog.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantResponse {
    String id;
    String productId;
    String productName;
    String color;
    Boolean active;
    String primaryImageUrl;
    List<String> imageUrls;
    List<VariantSizeResponse> sizes;
}
