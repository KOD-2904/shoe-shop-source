package com.ttthinh.shoe_shop_basic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vnpay")
@Data
public class VNPayConfig {
    private String tmnCode;
    private String hashSecret;
    private String paymentUrl;
    private String returnUrl;
    private String ipnUrl;
    private String refundUrl;
    private String refundCreateBy;
    private String version;
    private String command;
    private String orderType;
    private String currency;
    private String locale;
    private String timeZone = "Asia/Ho_Chi_Minh";
    private long paymentTimeoutMinutes = 15;

}
