package com.ttthinh.shoe_shop_basic.dto.response.auth;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String id;
    private String email;
    private String phone;
    private Set<String> providers;
    private boolean emailVerified;
    private String status;
    private Set<String> roles;
}
