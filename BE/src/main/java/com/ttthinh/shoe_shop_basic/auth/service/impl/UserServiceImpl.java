package com.ttthinh.shoe_shop_basic.auth.service.impl;

import com.ttthinh.shoe_shop_basic.auth.dto.request.ChangePasswordRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.ForgotPasswordRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.RegisterRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.ResetPasswordRequest;
import com.ttthinh.shoe_shop_basic.auth.entity.PasswordResetToken;
import com.ttthinh.shoe_shop_basic.auth.dto.response.UserResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.Role;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.auth.entity.EmailVerifyToken;
import com.ttthinh.shoe_shop_basic.auth.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.auth.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.auth.mapper.UserMapper;
import com.ttthinh.shoe_shop_basic.auth.repository.EmailVerifyRepository;
import com.ttthinh.shoe_shop_basic.auth.repository.PasswordResetTokenRepository;
import com.ttthinh.shoe_shop_basic.auth.repository.RoleRepository;
import com.ttthinh.shoe_shop_basic.auth.repository.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.auth.security.user.CustomUserDetails;
import com.ttthinh.shoe_shop_basic.auth.service.MailService;
import com.ttthinh.shoe_shop_basic.auth.service.RedisTokenService;
import com.ttthinh.shoe_shop_basic.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    @Value("${app.public-domain}")
    private String publicDomain;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailVerifyRepository emailVerifyRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final RedisTokenService redisTokenService;

    @Override
    @CacheEvict(value = "users", allEntries = true)
    public UserResponse register(RegisterRequest request) {
        var existingUser = userAccountRepository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            validatePhoneAvailable(request.getPhone(), existingUser.get().getId());
            return addLocalProviderToGoogleUser(existingUser.get(), request);
        }

        validatePhoneAvailable(request.getPhone(), null);

        Role roleUser = roleRepository.findByCode("ROLE_USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXIST));

        UserAccount userAccount = userMapper.toUser(request);
        userAccount.setPhone(blankToNull(request.getPhone()));
        userAccount.addProvider(AuthProvider.LOCAL);
        userAccount.setRoles(new HashSet<>(List.of(roleUser)));
        userAccount.setPassword(passwordEncoder.encode(request.getPassword()));
        userAccount.setEmailVerified(false);
        userAccount.setStatus(UserStatus.INACTIVE);

        userAccount = userAccountRepository.save(userAccount);

        String rawToken = UUID.randomUUID().toString();
        EmailVerifyToken emailVerifyToken = EmailVerifyToken.builder()
                .token(rawToken)
                .user(userAccount)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(30)))
                .used(false)
                .build();

        emailVerifyRepository.save(emailVerifyToken);
        mailService.sendMail(request.getEmail(), publicDomain.replaceAll("/+$", "") + "/auth/verify-email?token=" + rawToken);

        return userMapper.toUserResponse(userAccount);
    }

    private UserResponse addLocalProviderToGoogleUser(UserAccount userAccount, RegisterRequest request) {
        if (userAccount.hasProvider(AuthProvider.LOCAL)) {
            throw new AppException(ErrorCode.EMAIL_EXIST);
        }

        userAccount.addProvider(AuthProvider.LOCAL);
        userAccount.setPassword(passwordEncoder.encode(request.getPassword()));
        userAccount.setPhone(blankToNull(request.getPhone()));
        userAccount.setEmailVerified(true);
        userAccount.setStatus(UserStatus.ACTIVE);

        return userMapper.toUserResponse(userAccountRepository.save(userAccount));
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Cacheable(value = "users", key = "'all'")
    public List<UserResponse> getAllUsers() {
        return userAccountRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @PostAuthorize("returnObject.id == authentication.principal.id")
    public UserResponse getMyInformation() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        var user = userAccountRepository.findWithRolesAndProvidersById(userDetails.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
        return userMapper.toUserResponse(user);
    }

    @Override
    @PostAuthorize("returnObject.id == authentication.principal.id")
    @Cacheable(value = "userProfile", key = "'id:' + #id")
    public UserResponse getMyInformationById(String id) {
        UserAccount user = userAccountRepository.findWithRolesAndProvidersById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
        return userMapper.toUserResponse(user);
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userProfile", allEntries = true)
    })
    public void deleteAllUsers() {
        userAccountRepository.deleteAll();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userProfile", allEntries = true)
    })
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserAccount user = getCurrentUser();
        ensureLocalPasswordAccount(user);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);
        redisTokenService.revokeAllUserTokens(user.getId());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim();
        log.warn("Call forgot pw for email={}", email);

        var userAccount = userAccountRepository.findByEmail(email);
        if (userAccount.isEmpty()) {
            log.warn("Skip forgot pw because email is not registered");
            return;
        }

        UserAccount user = userAccount.get();
        ensureLocalProviderMetadata(user);
        if (!isLocalPasswordAccount(user)) {
            log.warn(
                    "Skip forgot pw for userId={} because account has no local password, providers={}, passwordPresent={}",
                    user.getId(),
                    user.getProviders(),
                    hasPassword(user)
            );
            return;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Skip forgot pw for userId={} because status is {}", user.getId(), user.getStatus());
            return;
        }

        if (!user.isEmailVerified()) {
            log.warn("Skip forgot pw for userId={} because email is not verified", user.getId());
            return;
        }

        expireUnusedPasswordResetTokens(user);

        String rawToken = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(rawToken)
                .user(user)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.warn("Created password reset token for userId={}", user.getId());
        mailService.sendPasswordResetMail(
                user.getEmail(),
                frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + rawToken
        );
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userProfile", allEntries = true)
    })
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.warn("Call reset pw");
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_VALID_TOKEN));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        UserAccount user = resetToken.getUser();
        ensureLocalPasswordAccount(user);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);

        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
        redisTokenService.revokeAllUserTokens(user.getId());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private UserAccount getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return userAccountRepository.findWithRolesAndProvidersById(userDetails.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
    }

    private void ensureLocalPasswordAccount(UserAccount user) {
        ensureLocalProviderMetadata(user);
        if (!isLocalPasswordAccount(user)) {
            throw new AppException(ErrorCode.PASSWORD_RESET_NOT_ALLOWED);
        }
    }

    private boolean isLocalPasswordAccount(UserAccount user) {
        return user.hasProvider(AuthProvider.LOCAL)
                && hasPassword(user);
    }

    private boolean hasPassword(UserAccount user) {
        return user.getPassword() != null && !user.getPassword().isBlank();
    }

    private void ensureLocalProviderMetadata(UserAccount user) {
        if (hasPassword(user) && !user.hasProvider(AuthProvider.LOCAL)) {
            user.addProvider(AuthProvider.LOCAL);
            userAccountRepository.save(user);
            log.warn("Repaired missing LOCAL provider for password userId={}", user.getId());
        }
    }

    private void expireUnusedPasswordResetTokens(UserAccount user) {
        Instant now = Instant.now();
        passwordResetTokenRepository.findByUserAndUsedFalse(user).forEach(token -> {
            token.setUsed(true);
            token.setUsedAt(now);
        });
    }

    private void validatePhoneAvailable(String phone, String currentUserId) {
        if (phone == null || phone.isBlank()) {
            return;
        }

        userAccountRepository.findByPhone(phone)
                .filter(user -> currentUserId == null || !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new AppException(ErrorCode.PHONE_EXIST);
                });
    }
}
