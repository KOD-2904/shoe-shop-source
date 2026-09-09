package com.ttthinh.shoe_shop_basic.dto.request.checkout;

import com.ttthinh.shoe_shop_basic.dto.request.checkout.ShippingFeeRequest;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@lombok.Data
public class CheckoutRequest {
    private String addressId;
    @NotEmpty(message = "Phải chọn ít nhất 1 sản phẩm")
    private List<String> cartItemIds;
    private Integer totalProductPrice;
    private ShippingFeeRequest shippingFeeRequest;
    private String voucherCode;
}
