package com.ttthinh.shoe_shop_basic.auth.controller;

import com.nimbusds.jose.JOSEException;
import com.ttthinh.shoe_shop_basic.auth.dto.request.ChangePasswordRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.ForgotPasswordRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.GoogleLoginRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.LoginRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.LogoutRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.ResetPasswordRequest;
import com.ttthinh.shoe_shop_basic.common.dto.ApiResponse;
import com.ttthinh.shoe_shop_basic.auth.dto.response.AuthResponse;
import com.ttthinh.shoe_shop_basic.auth.dto.response.LogoutResponse;
import com.ttthinh.shoe_shop_basic.auth.dto.response.TokenResponse;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.auth.security.auth.AuthCookieService;
import com.ttthinh.shoe_shop_basic.auth.service.AuthService;
import com.ttthinh.shoe_shop_basic.auth.service.MailService;
import com.ttthinh.shoe_shop_basic.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
//@CrossOrigin("http://127.0.0.1:5173/")
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MailService mailService;
    private final UserService userService;
    private final AuthCookieService authCookieService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws JOSEException {
        var authenticated = authService.login(loginRequest, httpRequest);
        authCookieService.addRefreshTokenCookie(httpResponse, authenticated.getRefreshToken());

        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .result(authenticated)
                .build();
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        var authenticated = authService.loginWithGoogle(request, httpRequest);
        authCookieService.addRefreshTokenCookie(httpResponse, authenticated.getRefreshToken());

        return ApiResponse.<AuthResponse>builder()
                .code(200)
                .result(authenticated)
                .build();
    }

//    @PostMapping("/introspect")
//    public ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospecRequest introspecRequest) throws ParseException, JOSEException {
//        var result  = authService.introspect(introspecRequest);
//        return ApiResponse.<IntrospectResponse>builder()
//                .result(result)
//                .build();
//    }

    @PostMapping("/log-out")
    public ApiResponse<LogoutResponse> logout(
            @RequestBody(required = false) LogoutRequest logoutRequest,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws ParseException, JOSEException {
        String refreshToken = resolveRefreshToken(logoutRequest, httpRequest);
        LogoutRequest request = logoutRequest != null ? logoutRequest : new LogoutRequest();
        request.setToken(refreshToken);
        LogoutResponse logoutResponse = authService.logout(request);
        authCookieService.clearRefreshTokenCookie(httpResponse);

        return ApiResponse.<LogoutResponse>builder()
                .code(200)
                .message("logged out successfully")
                .result(logoutResponse)
                .build();
    }

    @PostMapping("/refreshToken")
    public ApiResponse<TokenResponse> refreshToken(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) throws ParseException, JOSEException {
        String refreshToken = authCookieService.getRefreshToken(httpRequest)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_VALID_TOKEN));
        TokenResponse tokenResponse = authService.refreshToken(refreshToken, httpRequest);
        authCookieService.addRefreshTokenCookie(httpResponse, tokenResponse.getRefreshToken());

        return ApiResponse.<TokenResponse>builder()
                .code(200)
                .result(tokenResponse)
                .message("refresh token successfully")
                .build();
    }

    @GetMapping("/verify-email")
    public ApiResponse verifyResgiterToken(@RequestParam String token){
        mailService.verifyEmail(token);
        return ApiResponse.builder()
                .code(200)
                .message("email verified successfully")
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("password changed successfully")
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("If the email is registered with password login, a reset link has been sent")
                .build();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("password reset successfully")
                .build();
    }

    private String resolveRefreshToken(LogoutRequest logoutRequest, HttpServletRequest httpRequest) {
        if (logoutRequest != null && logoutRequest.getToken() != null && !logoutRequest.getToken().isBlank()) {
            return logoutRequest.getToken();
        }

        return authCookieService.getRefreshToken(httpRequest)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_VALID_TOKEN));
    }
}
