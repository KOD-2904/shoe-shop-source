package com.ttthinh.shoe_shop_basic.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {
    String accessToken;
    @JsonIgnore
    String refreshToken;
    String deviceId;
}
