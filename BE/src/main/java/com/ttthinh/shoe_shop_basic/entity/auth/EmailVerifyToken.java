package com.ttthinh.shoe_shop_basic.entity.auth;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "email_verify_token", indexes = {
        @Index(name = "idx_evt_token", columnList = "token", unique = true),
        @Index(name = "idx_evt_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class EmailVerifyToken extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    UserAccount user;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "used", nullable = false)
    boolean used = false;

    @Column(name = "used_at")
    Instant usedAt;
}

