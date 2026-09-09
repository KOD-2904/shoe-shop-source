package com.ttthinh.shoe_shop_basic.entity.catalog;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "variant_images",
        indexes = @Index(name = "idx_variant_image_variant", columnList = "variant_id"))
public class VariantImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    ProductVariant variant;

    @Column(nullable = false)
    String url;

    @Column(nullable = false)
    Boolean primaryImage = false;

    Integer sortOrder;
}
