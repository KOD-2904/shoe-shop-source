package com.ttthinh.shoe_shop_basic.shipping.dto.request;

import com.ttthinh.shoe_shop_basic.shipping.enums.GHNMode;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GHNSettingsRequest {
    private GHNMode mode;

    @Min(value = 0, message = "Mock shipping fee must be >= 0")
    private Integer mockFixedFee;
}
