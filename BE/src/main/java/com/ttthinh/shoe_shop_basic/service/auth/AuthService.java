package com.ttthinh.shoe_shop_basic.service.auth;

import com.nimbusds.jose.JOSEException;
import com.ttthinh.shoe_shop_basic.dto.request.auth.GoogleLoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LogoutRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.AuthResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.LogoutResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest) throws JOSEException;

    AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest);

    LogoutResponse logout(LogoutRequest logoutRequest);

    TokenResponse refreshToken(String refreshToken, HttpServletRequest httpRequest);
}
