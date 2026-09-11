package com.ttthinh.shoe_shop_basic.catalog.entity;

import com.ttthinh.shoe_shop_basic.common.entity.BaseEntity;
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
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, length = 150)
    String name;

    /**
     * Category cha
     * Ví dụ: Giày nam -> Giày sneaker
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    Category parent;

    /**
     * Category con
     */
    @OneToMany(mappedBy = "parent")
    Set<Category> children = new HashSet<>();
}
