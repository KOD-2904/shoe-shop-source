package com.ttthinh.shoe_shop_basic.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleLoginRequest {
    @NotBlank(message = "Google authorization code is required")
    String code;

    @NotBlank(message = "Redirect URI is required")
    String redirectUri;
}
