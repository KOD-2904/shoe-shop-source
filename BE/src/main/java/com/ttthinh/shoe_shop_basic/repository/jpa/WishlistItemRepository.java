package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.customer.WishlistItem;
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
