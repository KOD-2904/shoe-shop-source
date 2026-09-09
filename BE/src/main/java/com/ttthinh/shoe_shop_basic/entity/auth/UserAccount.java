package com.ttthinh.shoe_shop_basic.entity.auth;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "user_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_user_phone", columnNames = "phone")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserAccount extends BaseEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_provider",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Set<AuthProvider> providers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.INACTIVE;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "pk_user_role", columnNames = {"user_id", "role_id"})
            }
    )
    private Set<Role> roles = new HashSet<>();

    public boolean hasProvider(AuthProvider provider) {
        return provider != null && getProviders().contains(provider);
    }

    public void addProvider(AuthProvider provider) {
        if (provider != null) {
            getProviders().add(provider);
        }
    }

    public Set<AuthProvider> getProviders() {
        if (providers == null) {
            providers = new HashSet<>();
        }
        return providers;
    }

    public void setProviders(Set<AuthProvider> providers) {
        this.providers = providers != null ? providers : new HashSet<>();
    }
}
