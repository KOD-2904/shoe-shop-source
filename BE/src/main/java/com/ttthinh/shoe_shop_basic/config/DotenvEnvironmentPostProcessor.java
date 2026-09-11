package com.ttthinh.shoe_shop_basic.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = "dotenv-properties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            Map<String, Object> map = new HashMap<>();
            loadDotenv(map, ".");
            loadDotenv(map, "BE");

            MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
            environment.getPropertySources().addLast(propertySource);
        } catch (NoClassDefFoundError e) {
            // If dotenv library not present, skip quietly
        }
    }

    private void  loadDotenv(Map<String, Object> map, String directory) {
        if (!Files.exists(Path.of(directory, ".env"))) {
            return;
        }

        Dotenv dotenv = Dotenv.configure()
                .directory(directory)
                .ignoreIfMissing()
                .load();

        for (DotenvEntry entry : dotenv.entries()) {
            map.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }
}
