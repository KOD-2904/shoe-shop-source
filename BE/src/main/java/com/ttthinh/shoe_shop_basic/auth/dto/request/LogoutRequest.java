package com.ttthinh.shoe_shop_basic.auth.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogoutRequest {
    String token;
    boolean logoutAllDevices = false;
}
