package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.catalog.VariantImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VariantImageRepository extends JpaRepository<VariantImage, String> {
    @Query("""
        select coalesce(max(vi.sortOrder), 0)
        from VariantImage vi
        where vi.variant.id = :variantId
    """)
    int findMaxSortOrder(String variantId);

    @Modifying
    @Query("""
        update VariantImage vi
        set vi.primaryImage = false
        where vi.variant.id = :variantId
    """)
    void clearPrimaryByVariantId(String variantId);
}
