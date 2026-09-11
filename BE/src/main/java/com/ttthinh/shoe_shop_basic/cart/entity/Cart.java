package com.ttthinh.shoe_shop_basic.cart.entity;

import com.ttthinh.shoe_shop_basic.common.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "carts",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class Cart extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserAccount user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<CartItem> items;
}

