package com.ttthinh.shoe_shop_basic.order.dto.request;

import com.ttthinh.shoe_shop_basic.order.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateOrderStatusRequest {
    private OrderStatus status;
}
