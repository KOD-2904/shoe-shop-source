package com.ttthinh.shoe_shop_basic.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String PROPERTY_SOURCE_NAME = "dotenv-properties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> map = new HashMap<>();
            for (DotenvEntry entry : dotenv.entries()) {
                map.put(entry.getKey(), entry.getValue());
            }

            MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, map);
            environment.getPropertySources().addLast(propertySource);
        } catch (NoClassDefFoundError e) {
            // If dotenv library not present, skip quietly
        }
    }
}
