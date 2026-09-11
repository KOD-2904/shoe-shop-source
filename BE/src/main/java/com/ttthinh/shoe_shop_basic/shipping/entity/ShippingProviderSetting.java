package com.ttthinh.shoe_shop_basic.shipping.entity;

import com.ttthinh.shoe_shop_basic.common.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.shipping.enums.GHNMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "shipping_provider_settings")
public class ShippingProviderSetting extends BaseEntity {
    @Column(nullable = false, unique = true, length = 30)
    String provider;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    GHNMode mode;

    @Column(nullable = false)
    Integer mockFixedFee;
}
