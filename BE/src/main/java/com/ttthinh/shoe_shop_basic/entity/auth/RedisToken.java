package com.ttthinh.shoe_shop_basic.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisToken {
    private String id;
    private String userId;
    private String username;
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private String tokenType;
    private Date createdAt;
    private Date expiresAt;
    private Instant loginAt;
    private boolean active;

    public boolean isNotExpired() {
        return expiresAt != null && expiresAt.after(new Date());
    }

    public boolean isValid() {
        return active && isNotExpired();
    }

    public static RedisToken create(
            String jwtId,
            String userId,
            String username,
            String deviceId,
            String ipAddress,
            String userAgent,
            Date createdAt,
            Date expiresAt
    ) {
        return RedisToken.builder()
                .id(jwtId)
                .userId(userId)
                .username(username)
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .tokenType("REFRESH")
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .loginAt(Instant.now())
                .active(true)
                .build();
    }
}
