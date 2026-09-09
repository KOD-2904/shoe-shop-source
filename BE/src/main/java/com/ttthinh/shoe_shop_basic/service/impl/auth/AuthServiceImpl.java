package com.ttthinh.shoe_shop_basic.service.impl.auth;

import com.ttthinh.shoe_shop_basic.dto.request.auth.GoogleLoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.auth.LogoutRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.AuthResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.LogoutResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.TokenResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.RedisToken;
import com.ttthinh.shoe_shop_basic.entity.auth.Role;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.RoleRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.security.jwt.JwtService;
import com.ttthinh.shoe_shop_basic.security.user.CustomUserDetails;
import com.ttthinh.shoe_shop_basic.security.user.UserDetailServiceImpl;
import com.ttthinh.shoe_shop_basic.service.auth.AuthService;
import com.ttthinh.shoe_shop_basic.service.auth.RedisTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final RedisTokenService redisTokenService;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailServiceImpl userDetailsService;
    private final RestClient googleRestClient;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    @Value("${google.oauth.client-secret}")
    private String googleClientSecret;

    @Value("${google.oauth.token-uri}")
    private String googleTokenUri;

    @Value("${google.oauth.user-info-uri}")
    private String googleUserInfoUri;

    @Override
    public AuthResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getIdentifier(),
                            loginRequest.getPassword()
                    )
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            UserAccount userAccount = userDetails.getUser();

            if (!userAccount.hasProvider(AuthProvider.LOCAL)) {
                throw new AppException(ErrorCode.INVALID_LOGIN_PROVIDER);
            }
            if (userAccount.getStatus() != UserStatus.ACTIVE || !userAccount.isEmailVerified()) {
                throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
            }

            return createAuthResponse(authentication, userAccount, httpRequest);
        } catch (BadCredentialsException ex) {
            throw new AppException(ErrorCode.USERNAME_PASSWORD_NOT_MATCH);
        }
    }

    @Override
    public AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        GoogleProfile profile = fetchGoogleProfile(request);
        UserAccount userAccount = loadAuthReadyUser(getOrCreateGoogleUser(profile).getId());

        CustomUserDetails userDetails = new CustomUserDetails(userAccount);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        return createAuthResponse(authentication, userAccount, httpRequest);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        var claim = jwtService.parseToken(refreshToken);
        if (!Objects.equals(claim.getBody().get("tokenType", String.class), "REFRESH")) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        String jwtId = claim.getBody().getId();
        String deviceIdFromRequest = httpRequest.getRemoteAddr();

        RedisToken storedToken = redisTokenService.getRefreshTokenInfoById(jwtId);
        if (storedToken == null || !storedToken.isActive()) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }
        if (!storedToken.getDeviceId().equals(deviceIdFromRequest)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_DEVICE);
        }
        if (!redisTokenService.isValidRefreshToken(jwtId)) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        String email = claim.getBody().getSubject();
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);

        String accessToken = jwtService.generateAccessTokenFromUserDetails(userDetails, deviceIdFromRequest);
        String newRefreshToken = jwtService.generateRefreshTokenFromUserDetails(userDetails, deviceIdFromRequest);

        redisTokenService.saveRefreshToken(newRefreshToken, deviceIdFromRequest, httpRequest);
        redisTokenService.revokeRefreshToken(jwtId);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .deviceId(deviceIdFromRequest)
                .build();
    }

    @Override
    public LogoutResponse logout(LogoutRequest logoutRequest) {
        Jws<Claims> claimsJws;
        try {
            claimsJws = jwtService.parseToken(logoutRequest.getToken());
        } catch (JwtException e) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        var claims = claimsJws.getBody();
        String jwtId = claims.getId();
        String userId = claims.get("userId", String.class);
        String tokenType = claims.get("tokenType", String.class);

        if (!"REFRESH".equals(tokenType)) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        if (logoutRequest.isLogoutAllDevices()) {
            redisTokenService.revokeAllUserTokens(userId);
        } else {
            if (!redisTokenService.isValidRefreshToken(jwtId)) {
                throw new AppException(ErrorCode.NOT_VALID_TOKEN);
            }
            redisTokenService.revokeRefreshToken(jwtId);
        }

        return LogoutResponse.builder()
                .message("Logout successful")
                .success(true)
                .build();
    }

    private AuthResponse createAuthResponse(Authentication authentication, UserAccount userAccount, HttpServletRequest httpRequest) {
        String deviceId = httpRequest.getRemoteAddr();
        String accessToken = jwtService.generateAccessToken(authentication, deviceId);
        String refreshToken = jwtService.generateRefreshToken(authentication, deviceId);

        redisTokenService.saveRefreshToken(refreshToken, deviceId, httpRequest);

        return AuthResponse.builder()
                .email(userAccount.getEmail())
                .phone(userAccount.getPhone())
                .providers(mapProviders(userAccount.getProviders()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    private UserAccount createGoogleUser(GoogleProfile profile) {
        Role roleUser = roleRepository.findByCode("ROLE_USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));

        UserAccount user = new UserAccount();
        user.setEmail(profile.email());
        user.setGoogleId(profile.googleId());
        user.addProvider(AuthProvider.GOOGLE);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);

        HashSet<Role> roles = new HashSet<>();
        roles.add(roleUser);
        user.setRoles(roles);

        return userAccountRepository.save(user);
    }

    private UserAccount getOrCreateGoogleUser(GoogleProfile profile) {
        return userAccountRepository.findByEmail(profile.email())
                .map(user -> {
                    user.addProvider(AuthProvider.GOOGLE);
                    if (user.getGoogleId() == null || user.getGoogleId().isBlank()) {
                        user.setGoogleId(profile.googleId());
                    }
                    user.setEmailVerified(true);
                    user.setStatus(UserStatus.ACTIVE);
                    return userAccountRepository.save(user);
                })
                .orElseGet(() -> createGoogleUser(profile));
    }

    private UserAccount loadAuthReadyUser(String userId) {
        return userAccountRepository.findWithRolesAndProvidersById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
    }

    private GoogleProfile fetchGoogleProfile(GoogleLoginRequest request) {
        if (googleClientId == null || googleClientId.isBlank()
                || googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code", request.getCode());
            form.add("client_id", googleClientId);
            form.add("client_secret", googleClientSecret);
            form.add("redirect_uri", request.getRedirectUri());
            form.add("grant_type", "authorization_code");

            Map<?, ?> tokenResponse = googleRestClient.post()
                    .uri(googleTokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            String accessToken = getStringValue(tokenResponse, "access_token");
            if (accessToken == null || accessToken.isBlank()) {
                throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            Map<?, ?> userInfo = googleRestClient.get()
                    .uri(googleUserInfoUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(Map.class);

            String email = getStringValue(userInfo, "email");
            boolean emailVerified = Boolean.TRUE.equals(userInfo != null ? userInfo.get("email_verified") : null);
            if (email == null || email.isBlank() || !emailVerified) {
                throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            String googleId = getStringValue(userInfo, "sub");

            return new GoogleProfile(email, googleId);
        } catch (RestClientException ex) {
            log.warn("Google OAuth request failed: {}", ex.getMessage());
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    private String getStringValue(Map<?, ?> map, String key) {
        Object value = map != null ? map.get(key) : null;
        return value instanceof String text ? text : null;
    }

    private Set<String> mapProviders(Set<AuthProvider> providers) {
        if (providers == null) {
            return Set.of();
        }
        return providers.stream()
                .map(provider -> provider.name().toLowerCase())
                .collect(Collectors.toSet());
    }

    private record GoogleProfile(String email, String googleId) {
    }
}
