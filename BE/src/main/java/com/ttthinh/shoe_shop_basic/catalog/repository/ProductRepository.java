package com.ttthinh.shoe_shop_basic.catalog.repository;

import com.ttthinh.shoe_shop_basic.catalog.entity.Product;
import com.ttthinh.shoe_shop_basic.catalog.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findBySlug(String slug);
    List<Product> findByCategoryIdAndStatus(String categoryId, ProductStatus status);
    List<Product> findByBrandIdAndStatus(String brandId, ProductStatus status);
}
