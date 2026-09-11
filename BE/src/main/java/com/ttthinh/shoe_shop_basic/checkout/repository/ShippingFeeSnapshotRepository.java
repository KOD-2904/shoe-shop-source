package com.ttthinh.shoe_shop_basic.checkout.repository;

import com.ttthinh.shoe_shop_basic.checkout.entity.ShippingFeeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ShippingFeeSnapshotRepository extends JpaRepository<ShippingFeeSnapshot, String> {
    List<ShippingFeeSnapshot> findByUsedAtIsNullAndExpiresAtBefore(LocalDateTime now);
}
