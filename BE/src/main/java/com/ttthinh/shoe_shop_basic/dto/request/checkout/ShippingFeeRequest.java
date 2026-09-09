package com.ttthinh.shoe_shop_basic.dto.request.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeRequest {
    private Integer toDistrictId;
    private String toWardCode;
    private Integer weight;             // tổng cân nặng (gram)
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer insuranceValue; // gia tri bao hiem
}
