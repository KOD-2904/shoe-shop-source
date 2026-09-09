package com.ttthinh.shoe_shop_basic.entity.catalog;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "product_images",
        indexes = @Index(name = "idx_product_image_product", columnList = "product_id"))
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(nullable = false)
    String url;

    @Column(nullable = false)
    Boolean primaryImage = false;

    Integer sortOrder;
}
