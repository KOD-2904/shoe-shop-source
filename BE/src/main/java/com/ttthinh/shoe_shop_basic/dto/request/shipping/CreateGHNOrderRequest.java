package com.ttthinh.shoe_shop_basic.dto.request.shipping;

import com.ttthinh.shoe_shop_basic.dto.request.order.CreateOrderRequest;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGHNOrderRequest {
    private CreateOrderRequest createOrderRequest;
}
