package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.shipping.ShippingProviderSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingProviderSettingRepository extends JpaRepository<ShippingProviderSetting, String> {
    Optional<ShippingProviderSetting> findByProvider(String provider);
}
