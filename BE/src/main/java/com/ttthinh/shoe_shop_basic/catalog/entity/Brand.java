package com.ttthinh.shoe_shop_basic.catalog.entity;

import com.ttthinh.shoe_shop_basic.common.entity.BaseEntity;
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
@Table(name = "brands")
public class Brand extends BaseEntity{
    @Column(nullable = false, unique = true, length = 120)
    String name;

    String logoUrl;
}
