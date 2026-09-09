package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.request.auth.RegisterRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.UserResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.Role;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "providers", ignore = true)
    @Mapping(target = "googleId", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    UserAccount toUser(RegisterRequest request);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStringSet")
    @Mapping(target = "status", source = "status", qualifiedByName = "mapStatusToString")
    @Mapping(target = "providers", source = "providers", qualifiedByName = "mapProvidersToStringSet")
    UserResponse toUserResponse(UserAccount user);

    @Named("mapRolesToStringSet")
    default Set<String> mapRolesToStringSet(Set<Role> roles) {
        if (roles == null) {
            return new HashSet<>();
        }
        return roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());
    }

    @Named("mapStatusToString")
    default String mapStatusToString(UserStatus status) {
        return status != null ? status.name() : null;
    }

    default String mapProviderToString(AuthProvider provider) {
        return provider != null ? provider.name().toLowerCase() : null;
    }

    @Named("mapProvidersToStringSet")
    default Set<String> mapProvidersToStringSet(Set<AuthProvider> providers) {
        if (providers == null) {
            return new HashSet<>();
        }
        return providers.stream()
                .map(this::mapProviderToString)
                .collect(Collectors.toSet());
    }
}
