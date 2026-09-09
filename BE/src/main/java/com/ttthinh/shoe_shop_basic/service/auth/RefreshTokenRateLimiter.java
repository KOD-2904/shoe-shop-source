package com.ttthinh.shoe_shop_basic.service.auth;

import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefreshTokenRateLimiter {
    private static final String KEY_PREFIX = "auth:refresh_rate_limit:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); " +
                    "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
                    "return count;",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final long maxRequests;
    private final long windowSeconds;

    public RefreshTokenRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${auth.refresh-rate-limit.max-requests:10}") long maxRequests,
            @Value("${auth.refresh-rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public void checkAllowed(String clientAddress) {
        Long requestCount = redisTemplate.execute(
                INCREMENT_SCRIPT,
                List.of(KEY_PREFIX + clientAddress),
                String.valueOf(windowSeconds)
        );

        if (requestCount != null && requestCount > maxRequests) {
            throw new AppException(ErrorCode.TOO_MANY_REFRESH_REQUESTS);
        }
    }
}
