package com.ttthinh.shoe_shop_basic.promotion.repository;

import com.ttthinh.shoe_shop_basic.promotion.entity.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, String> {
    boolean existsByOrderIdAndVoucherCodeAndReleasedAtIsNull(String orderId, String voucherCode);

    Optional<VoucherUsage> findByOrderIdAndVoucherCodeAndReleasedAtIsNull(String orderId, String voucherCode);
}
