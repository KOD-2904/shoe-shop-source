package com.ttthinh.shoe_shop_basic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiConfig {
    private boolean enabled;
    private String apiKey;
    private String baseUrl;
    private String chatModel;
}
