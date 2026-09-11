package com.ttthinh.shoe_shop_basic.auth.service;

import com.nimbusds.jose.JOSEException;
import com.ttthinh.shoe_shop_basic.auth.dto.request.GoogleLoginRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.LoginRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.LogoutRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.response.AuthResponse;
import com.ttthinh.shoe_shop_basic.auth.dto.response.LogoutResponse;
import com.ttthinh.shoe_shop_basic.auth.dto.response.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest) throws JOSEException;

    AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest);

    LogoutResponse logout(LogoutRequest logoutRequest);

    TokenResponse refreshToken(String refreshToken, HttpServletRequest httpRequest);
}
