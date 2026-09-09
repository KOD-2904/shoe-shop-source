package com.ttthinh.shoe_shop_basic.dto.response.address;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressResponse {
    String id;
    Boolean isDefault;
    String receiverName;
    String phoneNumber;
    Integer provinceId;
    Integer districtId;
    String wardCode;
    String provinceName;
    String districtName;
    String wardName;
    String detailAddress;
    String fullAddress;
}
