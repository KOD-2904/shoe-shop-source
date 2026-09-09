package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.catalog.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
    @Query("""
        select coalesce(max(pi.sortOrder), 0)
        from ProductImage pi
        where pi.product.id = :productId
    """)
    int findMaxSortOrder(String productId);
    @Modifying
    @Query("""
        update ProductImage pi
        set pi.primaryImage = false
        where pi.product.id = :productId
    """)
    void clearPrimaryByProductId(String productId);
}
