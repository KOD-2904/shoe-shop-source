package com.ttthinh.shoe_shop_basic.entity.catalog;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "variant_sizes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_variant_size", columnNames = {"variant_id", "size"}),
                @UniqueConstraint(name = "uk_variant_size_sku", columnNames = {"sku"})
        }
)
public class VariantSize extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    ProductVariant variant;

    @Column(nullable = false, length = 50)
    String size;

    @Column(nullable = false, length = 100)
    String sku;

    @Column(nullable = false, precision = 18, scale = 2)
    BigDecimal price;

    @OneToOne(mappedBy = "variantSize", cascade = CascadeType.ALL, orphanRemoval = true)
    Inventory inventory;
}
