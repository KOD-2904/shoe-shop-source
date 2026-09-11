package com.ttthinh.shoe_shop_basic.auth.entity;

import com.ttthinh.shoe_shop_basic.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "permission",
        uniqueConstraints = @UniqueConstraint(name = "uk_permission_code", columnNames = "code")
)
public class Permission extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;
}
