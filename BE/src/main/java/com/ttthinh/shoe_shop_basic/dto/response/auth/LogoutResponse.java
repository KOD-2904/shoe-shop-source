package com.ttthinh.shoe_shop_basic.dto.response.auth;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogoutResponse {
    private boolean success;
    private String message;
    private Date timestamp;

    @Builder.Default
    private Date timeStamp = new Date();
}
