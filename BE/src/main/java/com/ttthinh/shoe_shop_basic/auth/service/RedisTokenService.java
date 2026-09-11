package com.ttthinh.shoe_shop_basic.auth.service;

import com.ttthinh.shoe_shop_basic.auth.entity.RedisToken;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;


public interface RedisTokenService {
    public void saveRefreshToken(String refreshToken,
                                 String deviceId, HttpServletRequest request);
    public boolean isValidRefreshToken(String jwtId);
    public RedisToken getRefreshTokenInfo(String refreshToken);
    public RedisToken getRefreshTokenInfoById(String jwtId);
    public void revokeRefreshToken(String refreshToken);
    public void revokeAllUserTokens(String userId);
    public List<RedisToken> getUserActiveSessions(String userId);
    public void deleteRefreshToken(String refreshToken);
}
