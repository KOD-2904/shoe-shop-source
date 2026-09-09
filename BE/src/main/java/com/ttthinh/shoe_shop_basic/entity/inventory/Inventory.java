package com.ttthinh.shoe_shop_basic.entity.inventory;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "inventories")
public class Inventory extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_size_id", nullable = false, unique = true)
    VariantSize variantSize;

    Integer quantity;

    @Builder.Default
    Integer quantityLocked = 0;

    @Version
    Long version;

    public int getAvailableQuantity() {
        return nullToZero(quantity) - nullToZero(quantityLocked);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
