package com.ttthinh.shoe_shop_basic.service.impl.auth;

import com.ttthinh.shoe_shop_basic.entity.auth.RedisToken;
import com.ttthinh.shoe_shop_basic.security.jwt.JwtProperties;
import com.ttthinh.shoe_shop_basic.security.jwt.JwtService;
import com.ttthinh.shoe_shop_basic.service.auth.RedisTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisTokenServiceImpl implements RedisTokenService {
    private static final String REFRESH_TOKEN_KEY = "auth:refresh_token:";
    private static final String USER_SESSIONS_KEY = "auth:user_sessions:";

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveRefreshToken(
            String refreshToken,
            String deviceId,
            HttpServletRequest request
    ) {
        var claims = jwtService.parseToken(refreshToken).getBody();
        String jwtId = claims.getId();
        String userId = claims.get("userId", String.class);

        RedisToken session = RedisToken.create(
                jwtId,
                userId,
                claims.getSubject(),
                deviceId,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                claims.getIssuedAt(),
                claims.getExpiration()
        );

        String tokenKey = tokenKey(jwtId);
        String userSessionsKey = userSessionsKey(userId);
        Duration ttl = Duration.ofMillis(jwtProperties.getRefreshTokenExpiration());

        redisTemplate.opsForValue().set(tokenKey, session, ttl);
        redisTemplate.opsForSet().add(userSessionsKey, jwtId);

        Long currentTtl = redisTemplate.getExpire(userSessionsKey);
        if (currentTtl == null || currentTtl < 0) {
            redisTemplate.expire(userSessionsKey, ttl);
        }

        log.info("Saved refresh token session for user {} with jti {}", userId, jwtId);
    }

    @Override
    public boolean isValidRefreshToken(String jwtId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey(jwtId)));
    }

    @Override
    public RedisToken getRefreshTokenInfo(String refreshToken) {
        var claims = jwtService.parseToken(refreshToken).getBody();
        return getRefreshTokenInfoById(claims.getId());
    }

    @Override
    public RedisToken getRefreshTokenInfoById(String jwtId) {
        Object value = redisTemplate.opsForValue().get(tokenKey(jwtId));
        if (value == null) {
            return null;
        }
        return (RedisToken) value;
    }

    @Override
    public void revokeRefreshToken(String jwtId) {
        RedisToken session = getRefreshTokenInfoById(jwtId);
        if (session == null) {
            return;
        }

        redisTemplate.delete(tokenKey(jwtId));
        redisTemplate.opsForSet().remove(userSessionsKey(session.getUserId()), jwtId);
        log.debug("Revoked refresh token session {}", jwtId);
    }

    @Override
    public void revokeAllUserTokens(String userId) {
        String userSessionsKey = userSessionsKey(userId);
        var jwtIds = redisTemplate.opsForSet().members(userSessionsKey);

        if (jwtIds != null) {
            for (Object jwtId : jwtIds) {
                redisTemplate.delete(tokenKey(String.valueOf(jwtId)));
            }
        }

        redisTemplate.delete(userSessionsKey);
    }

    @Override
    public List<RedisToken> getUserActiveSessions(String userId) {
        var jwtIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
        if (jwtIds == null || jwtIds.isEmpty()) {
            return List.of();
        }

        return jwtIds.stream()
                .map(jwtId -> getRefreshTokenInfoById(String.valueOf(jwtId)))
                .filter(Objects::nonNull)
                .filter(RedisToken::isValid)
                .toList();
    }

    @Override
    public void deleteRefreshToken(String userId) {
        revokeAllUserTokens(userId);
    }

    private String tokenKey(String jwtId) {
        return REFRESH_TOKEN_KEY + jwtId;
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY + userId;
    }
}
