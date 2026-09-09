package com.ttthinh.shoe_shop_basic.dto.request.order;

import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuyNowRequest {
    @NotNull(message = "Variant ID không được null")
    private String variantSizeId;

    @NotNull(message = "Số lượng không được null")
    private Integer quantity;

    @NotNull(message = "Phương thức thanh toán không được null")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Phải chọn địa chỉ giao hàng")
    private String addressId;

    private String shippingFeeSnapshotId;

    private String voucherCode;

    private String note;
}
