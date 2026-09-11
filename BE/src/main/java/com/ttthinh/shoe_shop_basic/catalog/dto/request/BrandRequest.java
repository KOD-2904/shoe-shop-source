package com.ttthinh.shoe_shop_basic.catalog.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrandRequest {
    @NotBlank
    @Size(max = 255)
    String name;
    @Size(max = 1024)
    String logoUrl; // tạm
}
