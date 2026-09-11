package com.ttthinh.shoe_shop_basic.catalog.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariantRequest {
    @NotBlank
    String productId;
    @NotBlank
    String color;
    Boolean active;
    @Valid
    @NotEmpty
    List<VariantSizeRequest> sizes;
}
