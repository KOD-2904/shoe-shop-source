package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    @Query("""
            select item
            from OrderItem item
            join fetch item.order o
            join fetch item.variantSize size
            join fetch size.variant variant
            join fetch variant.product product
            where item.id = :id
            """)
    Optional<OrderItem> findDetailById(@Param("id") String id);
}
