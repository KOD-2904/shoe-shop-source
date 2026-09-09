package com.ttthinh.shoe_shop_basic.dto.response.cart;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemResponse {
    private String cartItemId;
    private String variantSizeId;

    private String productName;
    private String imageUrl;
    private String brand;
    private String category;
    private String size;
    private String color;

    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discountPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
