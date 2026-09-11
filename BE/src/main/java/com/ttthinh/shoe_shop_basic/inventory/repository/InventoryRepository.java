package com.ttthinh.shoe_shop_basic.inventory.repository;

import com.ttthinh.shoe_shop_basic.inventory.entity.Inventory;
import com.ttthinh.shoe_shop_basic.catalog.entity.VariantSize;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Inventory getInventoryByVariantSize_Id(String variantSizeId);

    Optional<Inventory> findByVariantSizeId(String variantSizeId);

    Optional<Inventory> findByVariantSize(VariantSize variantSize);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.variantSize.id = :variantSizeId")
    Optional<Inventory> findLockedByVariantSizeId(@Param("variantSizeId") String variantSizeId);

    @Query("""
            select i from Inventory i
            join fetch i.variantSize size
            join fetch size.variant variant
            join fetch variant.product product
            order by product.name asc, variant.color asc, size.size asc
            """)
    List<Inventory> findAllWithCatalog();

    @Query("""
            select i from Inventory i
            join fetch i.variantSize size
            join fetch size.variant variant
            join fetch variant.product product
            where (coalesce(i.quantity, 0) - coalesce(i.quantityLocked, 0)) <= :threshold
            order by (coalesce(i.quantity, 0) - coalesce(i.quantityLocked, 0)) asc, product.name asc
            """)
    List<Inventory> findLowStockWithCatalog(@Param("threshold") int threshold);
}
