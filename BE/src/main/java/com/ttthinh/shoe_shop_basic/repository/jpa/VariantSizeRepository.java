package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VariantSizeRepository extends JpaRepository<VariantSize, String> {
    List<VariantSize> findByVariantId(String variantId);
}
