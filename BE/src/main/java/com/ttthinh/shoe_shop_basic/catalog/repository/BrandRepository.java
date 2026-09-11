package com.ttthinh.shoe_shop_basic.catalog.repository;

import com.ttthinh.shoe_shop_basic.catalog.entity.Brand;
import com.ttthinh.shoe_shop_basic.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, String> {
    boolean existsByName(String name);
}
