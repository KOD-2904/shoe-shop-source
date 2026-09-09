package com.ttthinh.shoe_shop_basic.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    boolean authenticated;
    String accessToken;
    @JsonIgnore
    String refreshToken;
    String email;
    String phone;
    Set<String> providers;
}
