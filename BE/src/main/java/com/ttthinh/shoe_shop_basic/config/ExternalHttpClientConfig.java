package com.ttthinh.shoe_shop_basic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class ExternalHttpClientConfig {
    @Bean
    public SimpleClientHttpRequestFactory externalRequestFactory(
            @Value("${external-http.connect-timeout-ms:5000}") long connectTimeoutMs,
            @Value("${external-http.read-timeout-ms:10000}") long readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return factory;
    }

    @Bean
    public RestTemplate restTemplate(SimpleClientHttpRequestFactory externalRequestFactory) {
        return new RestTemplate(externalRequestFactory);
    }

    @Bean
    public RestClient restClient(SimpleClientHttpRequestFactory externalRequestFactory) {
        return RestClient.builder()
                .requestFactory(externalRequestFactory)
                .build();
    }
}
