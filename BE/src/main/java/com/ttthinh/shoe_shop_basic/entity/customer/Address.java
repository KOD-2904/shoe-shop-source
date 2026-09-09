package com.ttthinh.shoe_shop_basic.entity.customer;
//
import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@Table(name = "address")
@AllArgsConstructor
@NoArgsConstructor
public class Address extends BaseEntity{
    private String userId;
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



//    order.setPhoneNumber(address.getPhoneNumber());
//        order.setReceiverName(address.getReceiverName());
}

