package com.ttthinh.shoe_shop_basic.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductReviewRequest {
    @NotNull
    @Min(1)
    @Max(5)
    Integer rating;

    @Size(max = 1000)
    String comment;

    String orderItemId;

    @Size(max = 5)
    List<@Size(max = 1024) String> imageUrls;
}
