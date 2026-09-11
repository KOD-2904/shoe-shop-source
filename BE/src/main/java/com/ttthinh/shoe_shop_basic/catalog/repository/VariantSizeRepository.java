package com.ttthinh.shoe_shop_basic.catalog.repository;

import com.ttthinh.shoe_shop_basic.catalog.entity.VariantSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VariantSizeRepository extends JpaRepository<VariantSize, String> {
    List<VariantSize> findByVariantId(String variantId);
}
