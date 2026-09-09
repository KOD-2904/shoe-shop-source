package com.ttthinh.shoe_shop_basic.controller.auth;

import com.nimbusds.jose.JOSEException;
import com.ttthinh.shoe_shop_basic.dto.request.auth.GoogleLoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LogoutRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.AuthResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.LogoutResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.TokenResponse;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.security.auth.AuthCookieService;
import com.ttthinh.shoe_shop_basic.service.auth.AuthService;
import com.ttthinh.shoe_shop_basic.service.auth.MailService;
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

    private String resolveRefreshToken(LogoutRequest logoutRequest, HttpServletRequest httpRequest) {
        if (logoutRequest != null && logoutRequest.getToken() != null && !logoutRequest.getToken().isBlank()) {
            return logoutRequest.getToken();
        }

        return authCookieService.getRefreshToken(httpRequest)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_VALID_TOKEN));
    }
}
