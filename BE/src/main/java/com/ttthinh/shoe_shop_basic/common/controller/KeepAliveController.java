package com.ttthinh.shoe_shop_basic.common.controller;

import com.ttthinh.shoe_shop_basic.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class KeepAliveController {

    @GetMapping("/keepalive")
    public ApiResponse<Map<String, String>> keepAlive() {
        return ApiResponse.<Map<String, String>>builder()
                .code(200)
                .message("success")
                .result(Map.of(
                        "status", "UP",
                        "service", "shoe-shop-api",
                        "timestamp", Instant.now().toString()
                ))
                .build();
    }
}
