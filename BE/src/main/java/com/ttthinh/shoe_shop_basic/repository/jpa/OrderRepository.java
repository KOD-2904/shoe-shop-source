package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    @EntityGraph(attributePaths = {"items", "items.variantSize", "items.variantSize.variant", "items.variantSize.variant.product"})
    List<Order> findByUserOrderByCreatedAtDesc(UserAccount user);

    @EntityGraph(attributePaths = {"items", "items.variantSize", "items.variantSize.variant", "items.variantSize.variant.product"})
    Page<Order> findByUser(UserAccount user, Pageable pageable);

    //List<Order> findOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"items", "items.variantSize", "items.variantSize.variant", "items.variantSize.variant.product"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"items", "items.variantSize", "items.variantSize.variant", "items.variantSize.variant.product"})
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByIdAndUser(String id, UserAccount user);

    Optional<Order> findOrderById(String id);
    List<Order> findByStatus(OrderStatus status);
    Optional<Order> findByShippingOrderCode(String shippingOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.shippingOrderCode = :shippingOrderCode")
    Optional<Order> findByShippingOrderCodeForUpdate(@Param("shippingOrderCode") String shippingOrderCode);

    @Query("""
            select count(o) > 0
            from Order o
            join o.items item
            where o.user = :user
              and o.status = com.ttthinh.shoe_shop_basic.enums.OrderStatus.DELIVERED
              and item.variantSize.variant.product.id = :productId
            """)
    boolean existsDeliveredOrderForProduct(@Param("user") UserAccount user, @Param("productId") String productId);

}
