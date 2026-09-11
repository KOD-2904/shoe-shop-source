package com.ttthinh.shoe_shop_basic.catalog.repository;

import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.catalog.entity.Product;
import com.ttthinh.shoe_shop_basic.catalog.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, String> {
    List<ProductReview> findByProductOrderByCreatedAtDesc(Product product);

    Optional<ProductReview> findTopByUserAndProductAndOrderItemIsNullOrderByCreatedAtDesc(UserAccount user, Product product);

    Optional<ProductReview> findByOrderItemId(String orderItemId);

    boolean existsByOrderItemId(String orderItemId);
}
