package com.ttthinh.shoe_shop_basic.shipping.service;

import com.ttthinh.shoe_shop_basic.customer.entity.Address;
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
