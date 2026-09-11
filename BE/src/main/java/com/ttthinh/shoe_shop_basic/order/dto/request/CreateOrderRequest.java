package com.ttthinh.shoe_shop_basic.order.dto.request;

import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {
    @NotEmpty(message = "Phải chọn ít nhất 1 sản phẩm")
    private List<String> cartItemIds;     // ID của CartItem được chọn

    @NotNull(message = "Phải chọn địa chỉ giao hàng")
    private String addressId;

    @NotNull(message = "Phai co shipping fee snapshot")
    private String shippingFeeSnapshotId;

    @NotNull(message = "Phải chọn phương thức thanh toán")
    private PaymentMethod paymentMethod;

    private String voucherCode;

    private String note;
}
