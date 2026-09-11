package com.ttthinh.shoe_shop_basic.shipping.dto.request;

import com.ttthinh.shoe_shop_basic.order.dto.request.CreateOrderRequest;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGHNOrderRequest {
    private CreateOrderRequest createOrderRequest;
}
