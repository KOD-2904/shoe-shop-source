package com.ttthinh.shoe_shop_basic.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
@Slf4j
public class AppTimeZoneConfig {
    private static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";

    private final ZoneId appZoneId;

    public AppTimeZoneConfig(@Value("${app.time-zone:" + DEFAULT_TIME_ZONE + "}") String timeZone) {
        this.appZoneId = resolve(timeZone);
        TimeZone.setDefault(TimeZone.getTimeZone(appZoneId));
    }

    @PostConstruct
    void configureDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(appZoneId));
        log.info("Application time zone set to {}", appZoneId);
    }

    @Bean
    ZoneId appZoneId() {
        return appZoneId;
    }

    private ZoneId resolve(String timeZone) {
        String value = StringUtils.hasText(timeZone) ? timeZone.trim() : DEFAULT_TIME_ZONE;
        try {
            return ZoneId.of(value);
        } catch (DateTimeException exception) {
            throw new IllegalStateException("Invalid app.time-zone: " + value, exception);
        }
    }
}
