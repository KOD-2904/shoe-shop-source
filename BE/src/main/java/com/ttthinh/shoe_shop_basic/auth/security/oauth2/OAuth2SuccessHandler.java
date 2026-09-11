package com.ttthinh.shoe_shop_basic.auth.security.oauth2;

import com.ttthinh.shoe_shop_basic.auth.entity.Role;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.auth.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.auth.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.auth.repository.RoleRepository;
import com.ttthinh.shoe_shop_basic.auth.repository.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.auth.security.auth.AuthCookieService;
import com.ttthinh.shoe_shop_basic.auth.security.jwt.JwtService;
import com.ttthinh.shoe_shop_basic.auth.security.user.CustomUserDetails;
import com.ttthinh.shoe_shop_basic.auth.service.RedisTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final AuthCookieService authCookieService;

    @Value("${app.oauth2-success-url}")
    private String oauth2SuccessUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");
            String googleId = oauthUser.getAttribute("sub");
            Boolean emailVerified = oauthUser.getAttribute("email_verified");

            if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
                redirectWithError(request, response, ErrorCode.GOOGLE_AUTH_FAILED);
                return;
            }

            UserAccount user = loadAuthReadyUser(getOrCreateGoogleUser(email, googleId).getId());

            CustomUserDetails userDetails = new CustomUserDetails(user);
            Authentication jwtAuthentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            String deviceId = request.getRemoteAddr();
            String refreshToken = jwtService.generateRefreshToken(jwtAuthentication, deviceId);

            redisTokenService.saveRefreshToken(refreshToken, deviceId, request);
            authCookieService.addRefreshTokenCookie(response, refreshToken);
            clearAuthenticationAttributes(request);
            new SecurityContextLogoutHandler().logout(request, response, authentication);

            getRedirectStrategy().sendRedirect(request, response, oauth2SuccessUrl);
        } catch (AppException ex) {
            redirectWithError(request, response, ex.getErrorCode());
        }
    }

    private UserAccount getOrCreateGoogleUser(String email, String googleId) {
        return userAccountRepository.findByEmail(email)
                .map(user -> {
                    user.addProvider(AuthProvider.GOOGLE);
                    if (user.getGoogleId() == null || user.getGoogleId().isBlank()) {
                        user.setGoogleId(googleId);
                    }
                    user.setEmailVerified(true);
                    user.setStatus(UserStatus.ACTIVE);
                    return userAccountRepository.save(user);
                })
                .orElseGet(() -> createGoogleUser(email, googleId));
    }

    private UserAccount loadAuthReadyUser(String userId) {
        return userAccountRepository.findWithRolesAndProvidersById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
    }

    private void redirectWithError(
            HttpServletRequest request,
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(oauth2SuccessUrl)
                .queryParam("oauth2Error", errorCode.name())
                .queryParam("message", errorCode.getMessage())
                .build()
                .encode()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private UserAccount createGoogleUser(String email, String googleId) {
        Role roleUser = roleRepository.findByCode("ROLE_USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));

        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setGoogleId(googleId);
        user.addProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(new HashSet<>(List.of(roleUser)));

        return userAccountRepository.save(user);
    }
}
