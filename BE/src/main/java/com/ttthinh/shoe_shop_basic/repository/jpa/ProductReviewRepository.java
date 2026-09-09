package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductReview;
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
