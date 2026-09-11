package com.ttthinh.shoe_shop_basic.customer.repository;

import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.catalog.entity.Product;
import com.ttthinh.shoe_shop_basic.customer.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, String> {
    List<WishlistItem> findByUserOrderByCreatedAtDesc(UserAccount user);

    Optional<WishlistItem> findByUserAndProduct(UserAccount user, Product product);

    boolean existsByUserAndProduct(UserAccount user, Product product);
}
