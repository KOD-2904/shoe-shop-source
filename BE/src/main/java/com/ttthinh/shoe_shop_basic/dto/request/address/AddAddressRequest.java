package com.ttthinh.shoe_shop_basic.dto.request.address;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class AddAddressRequest {
    private Boolean isDefault = false;
    private String receiverName;
    private String phoneNumber;

    // Địa chỉ cũ (dùng cho GHN)
    private Integer provinceId;      // ID tỉnh
    private Integer districtId;      // ID quận/huyện
    private String wardCode;         // ID phường/xã (string!)

    // Text hiển thị
    private String provinceName;
    private String districtName;
    private String wardName;
    private String detailAddress;

    @Column(columnDefinition = "TEXT")
    private String fullAddress;
}
