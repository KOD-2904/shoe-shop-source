package com.ttthinh.shoe_shop_basic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = DataRedisRepositoriesAutoConfiguration.class)
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.ttthinh.shoe_shop_basic.repository.jpa")
public class ShoeShopBasicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoeShopBasicApplication.class, args);
    }
}
