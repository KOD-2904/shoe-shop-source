package com.ttthinh.shoe_shop_basic.service.impl.auth;

import com.ttthinh.shoe_shop_basic.dto.request.auth.RegisterRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.UserResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.Role;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.auth.EmailVerifyToken;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.UserMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.EmailVerifyRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.RoleRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.security.user.CustomUserDetails;
import com.ttthinh.shoe_shop_basic.service.auth.MailService;
import com.ttthinh.shoe_shop_basic.service.auth.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String BASE_URL = "http://localhost:8080";

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailVerifyRepository emailVerifyRepository;
    private final MailService mailService;

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
        mailService.sendMail(request.getEmail(), BASE_URL + "/auth/verify-email?token=" + rawToken);

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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
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
