package com.ttthinh.shoe_shop_basic.dto.response.customer;

import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WishlistItemResponse {
    String id;
    String productId;
    LocalDateTime createdAt;
    ProductResponse product;
}
