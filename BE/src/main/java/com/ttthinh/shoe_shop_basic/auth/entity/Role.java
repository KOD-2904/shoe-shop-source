package com.ttthinh.shoe_shop_basic.auth.entity;

import com.ttthinh.shoe_shop_basic.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "role",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_role_code", columnNames = "code")
        }
)

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Role extends BaseEntity {
    @Column(name = "code", nullable = false, length = 100)
    private String code;      // VD: ROLE_ADMIN, ROLE_USER

    @Column(name = "name", nullable = false, length = 255)
    private String name;      // Tên hiển thị

    @Column(name = "description", length = 500)
    private String description;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<UserAccount> users = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id", referencedColumnName = "id"),
            uniqueConstraints = @UniqueConstraint(name = "pk_role_permission", columnNames = {"role_id", "permission_id"})
    )
    private Set<Permission> permissions = new HashSet<>();
}
