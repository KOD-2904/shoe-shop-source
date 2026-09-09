package com.ttthinh.shoe_shop_basic.service.shipping;

import com.ttthinh.shoe_shop_basic.entity.customer.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GHNShopInfo {
    private Integer shopId;
    private String token;
    private Address fromAddress;
}
