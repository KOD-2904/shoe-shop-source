package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.catalog.Category;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    List<ProductVariant> findByProductId(String productId);
}
