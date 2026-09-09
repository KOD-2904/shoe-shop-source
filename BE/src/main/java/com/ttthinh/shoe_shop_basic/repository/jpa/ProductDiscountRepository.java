package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.promotion.ProductDiscount;
import com.ttthinh.shoe_shop_basic.enums.PromotionTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ProductDiscountRepository extends JpaRepository<ProductDiscount, String> {
    @Query("""
            select d
            from ProductDiscount d
            where d.active = true
              and d.targetType in :targetTypes
              and d.targetId in :targetIds
              and (d.startsAt is null or d.startsAt <= :now)
              and (d.endsAt is null or d.endsAt >= :now)
            """)
    List<ProductDiscount> findActiveCandidates(
            @Param("targetTypes") Collection<PromotionTargetType> targetTypes,
            @Param("targetIds") Collection<String> targetIds,
            @Param("now") LocalDateTime now
    );
}
