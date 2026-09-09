package com.ttthinh.shoe_shop_basic.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ProductionStartupValidation implements ApplicationRunner {
    private static final String DEFAULT_JWT_SECRET = "local-development-secret-key-change-me-please-32-characters-minimum";
    private static final String DEFAULT_ADMIN_PASSWORD = "ChangeMe123!";
    private static final String DEFAULT_DEMO_PASSWORD = "ChangeMe123!";

    private final Environment environment;

    @Value("${app.env:local}")
    private String appEnv;

    @Value("${app.init.enabled:true}")
    private boolean initEnabled;

    @Value("${app.init.admin-password:}")
    private String adminPassword;

    @Value("${app.init.demo-password:}")
    private String demoPassword;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public void run(ApplicationArguments args) {
        if (!isProduction()) {
            return;
        }
        require(!initEnabled, "app.init.enabled must be false in production");
        require(jwtSecret != null && jwtSecret.length() >= 32 && !DEFAULT_JWT_SECRET.equals(jwtSecret),
                "JWT_SECRET must be set to a non-default secret with at least 32 characters in production");
        require(!DEFAULT_ADMIN_PASSWORD.equals(adminPassword),
                "APP_INIT_ADMIN_PASSWORD must not use the local default in production");
        require(!DEFAULT_DEMO_PASSWORD.equals(demoPassword),
                "APP_INIT_DEMO_PASSWORD must not use the local default in production");
    }

    private boolean isProduction() {
        String normalizedEnv = normalize(appEnv);
        return "prod".equals(normalizedEnv)
                || "production".equals(normalizedEnv)
                || Arrays.stream(environment.getActiveProfiles())
                .map(this::normalize)
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Production configuration error: " + message);
        }
    }
}
