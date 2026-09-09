package com.ttthinh.shoe_shop_basic.dto.request.catalog;

import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductRequest {
    @NotBlank
    @Size(max = 255)
    String name;
    @NotBlank
    @Size(max = 255)
    String slug;
    String description;
    String brandId;
    String categoryId;
    @DecimalMin(value = "0.01")
    BigDecimal basePrice;
    //String status; // DRAFT | ACTIVE | INACTIVE
}
