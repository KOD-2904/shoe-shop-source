package com.ttthinh.shoe_shop_basic.inventory.dto.response;

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
public class InventoryListItemResponse {
    String inventoryId;
    String productId;
    String productName;
    String variantId;
    String color;
    String variantSizeId;
    String size;
    String sku;
    BigDecimal price;
    Integer quantity;
    Integer quantityLocked;
    Integer availableQuantity;
    Boolean lowStock;
    Boolean outOfStock;
}
