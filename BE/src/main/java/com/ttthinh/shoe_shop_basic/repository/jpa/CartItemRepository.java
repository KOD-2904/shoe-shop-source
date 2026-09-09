package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.cart.Cart;
import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String > {
    Optional<CartItem> findByCartAndVariantSize(Cart cart, VariantSize variantSize);

    List<CartItem> findByCart(Cart cart);

    void deleteByCart(Cart cart);

}
