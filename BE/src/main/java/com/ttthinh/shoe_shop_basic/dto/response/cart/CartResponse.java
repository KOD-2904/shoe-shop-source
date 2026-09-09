package com.ttthinh.shoe_shop_basic.dto.response.cart;

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
public class CartResponse {
    private String cartId;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
}
