package com.ttthinh.shoe_shop_basic.shipping.repository;

import com.ttthinh.shoe_shop_basic.shipping.entity.ShippingProviderSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingProviderSettingRepository extends JpaRepository<ShippingProviderSetting, String> {
    Optional<ShippingProviderSetting> findByProvider(String provider);
}
